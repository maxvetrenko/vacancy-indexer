package com.maxvetrenko.vacancy;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.maxvetrenko.vacancy.domain.Vacancy;
import com.google.common.collect.Lists;
import com.maxvetrenko.vacancy.db.VacanciesDao;
import com.maxvetrenko.vacancy.utils.RetryActionsRunner;
import com.maxvetrenko.vacancy.utils.RetryActionsRunner.ExceptionThrowingAction;

@Component
public final class VacanciesSaver {

    private static final Logger LOG = LoggerFactory.getLogger(VacanciesSaver.class);

    private static final int MAX_PARALLEL_REQUESTS_COUNT = 4;

    private static final String HTTP_ADDRESS = "http://rabota.ua";

    private static final String NON_BREAKABLE_SPACE = "\u00A0";

    private int maxReconnectCount = 4;

    private int timeToWaitBeforeReconnect = 5000;

    private VacanciesDao vacanciesDao;

    /**
     * @return time to wait before reconnect in ms.
     */
    public int getTimeToWaitBeforeReconnect() {
        return timeToWaitBeforeReconnect;
    }

    /**
     * Sets time to wait before reconnect in ms.
     */
    public void setTimeToWaitBeforeReconnect(int timeToWaitBeforeReconnect) {
        this.timeToWaitBeforeReconnect = timeToWaitBeforeReconnect;
    }

    /**
     * @return max count of reconnections.
     */
    public int getMaxReconnectCount() {
        return maxReconnectCount;
    }

    /**
     * Sets max count of reconnections.
     */
    public void setMaxReconnectCount(int maxReconnectCount) {
        this.maxReconnectCount = maxReconnectCount;
    }

    @Inject
    public void setDao(VacanciesDao dao) {
        this.vacanciesDao = dao;
    }

    /**
     * Starts processing vacancies.
     */
    public void run() {
        LOG.info("Processing vacancies at '{}' started", HTTP_ADDRESS);
        reloadVacancies();
        LOG.info("Processing vacancies at '{}' finished, results were commited", HTTP_ADDRESS);
    }

    private void reloadVacancies() {

        vacanciesDao.clearVacancyTable();

        try {
            reloadVacanciesInternal();
        } catch (IOException e) {
            throw new RuntimeException("Error while processing vacncies at " + HTTP_ADDRESS, e);
        }

        vacanciesDao.updateFullText();
    }

    private void reloadVacanciesInternal() throws IOException {
        Document mainPage = Jsoup.connect(HTTP_ADDRESS
                + "/%D0%B2%D0%B0%D0%BA%D0%B0%D0%BD%D1%81%D0%B8%D0%B8").get();
        Elements groups = mainPage.select("a[data-id]:not(a[style])");

        for (int i = 0; i < groups.size(); i++) {

            Element groupLink = groups.get(i);
            String groupName = groupLink.text();

            String groupAbsoluteUri = HTTP_ADDRESS + groupLink.attr("href");

            LOG.info("Parsing vacancies for group: '{}' ...", groupName);

            processVacancyGroup(groupAbsoluteUri, groupName);
        }
    }

    private void processVacancyGroup(String vacancyGroupUri,
            String vacancyGroupName) throws IOException {

        LOG.info("Scanning for vacancies at group {} ... ", vacancyGroupName);

        List<String> vacancyUrls = scanForVacancies(vacancyGroupUri);

        LOG.info("Scanning for vacancies at group '{}' finished, {} vacancies found",
                vacancyGroupName, vacancyUrls.size());

        List<Vacancy> loadedVacancies = loadVacancies(vacancyUrls);

        vacanciesDao.saveVacanciesToDb(loadedVacancies);
    }

    private static List<String> scanForVacancies(String groupUri)
            throws IOException {

        Document groupPage = Jsoup.connect(groupUri).get();

        Elements vacancyLinks = groupPage.select("a[class=t]");

        List<String> vacancyUrls = Lists.newLinkedList();

        for (int i = 0; i < vacancyLinks.size(); i++) {

            Element vacancyLink = vacancyLinks.get(i);

            String fullVacancyUri = HTTP_ADDRESS + vacancyLink.attr("href");
            vacancyUrls.add(fullVacancyUri);
        }

        return vacancyUrls;
    }

    private List<Vacancy> loadVacancies(List<String> vacancyUrls) {

        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

        taskExecutor.setThreadNamePrefix("Thread #");
        taskExecutor.setConcurrencyLimit(MAX_PARALLEL_REQUESTS_COUNT);

        Set<Future<Vacancy>> tasks = new HashSet<>();

        for (final String vacancyUrl : vacancyUrls) {
            try {
                Future<Vacancy> task = taskExecutor.submit(new Callable<Vacancy>() {
                    @Override
                    public Vacancy call() throws Exception {
                        return loadVacancy(vacancyUrl);
                    }
                });
                LOG.info("Creating task with vacancy {}", task.get());
                tasks.add(task);
            } catch (InterruptedException | ExecutionException unparseableVacancy) {
                LOG.warn("Cannot create task because vacancy {} is unparsable", vacancyUrl);
            }
        }

        return waitForAllVacancyLoadTasksToComplete(tasks);
    }
    
    private static Vacancy loadVacancy(String vacancyUrl) throws IOException,
            ParseException {

        LOG.info("Loading info for vacancy {} started", vacancyUrl);

        Document vacancyPage = Jsoup.connect(vacancyUrl).get();

        Element vacancyInfoDiv = vacancyPage.select("div.rua-l-wrapper2").first();

        String vacancyName = extractText(vacancyInfoDiv, "h1.VacancyTitle*");

        // Name and Url
        Vacancy vacancy = new Vacancy(vacancyUrl, vacancyName);

        // Date
        Element date = vacancyPage.select("span[itemprop=datePosted]").first();
        String publicationDateStr = date.text();
        String publicationDateStrNormalized = publicationDateStr.replaceAll(
                NON_BREAKABLE_SPACE, "");
        Date publicationDate = new SimpleDateFormat("yyyy-MM-dd")
                .parse(publicationDateStrNormalized);
        vacancy.setPublicationDate(new java.sql.Date(publicationDate.getTime()));

        // company
        vacancy.setCompany(extractText(vacancyInfoDiv, "span[itemprop=name]"));

        // industry
        vacancy.setIndustry(extractText(vacancyInfoDiv, "span[itemprop=industry]"));

        // jobLocation
        vacancy.setJobLocation(extractText(vacancyInfoDiv,
                "span[itemprop=addressLocality]"));

        // employmentType
        vacancy.setEmploymentType(extractText(vacancyInfoDiv,
                "span[itemprop=employmentType]"));

        LOG.info("Loading info for vacancy {} finished", vacancyUrl);

        return vacancy;
    }

    private static String extractText(Element element, String cssQuery) {

        String result = "";

        if (element != null) {
            Elements selection = element.select(cssQuery);
            if (selection.hasText()) {
                result = selection.text();
            }
        }

        return result;
    }

    private List<Vacancy> waitForAllVacancyLoadTasksToComplete(
            Set<Future<Vacancy>> tasks) {

        List<Vacancy> loadedVacancies = new LinkedList<Vacancy>();

        for (Future<Vacancy> task : tasks) {

            LoadVacancyAction loadVacancyAction = new LoadVacancyAction(task);

            RetryActionsRunner.executeWithRetryAttempts(
                    loadVacancyAction, getMaxReconnectCount(),
                    getTimeToWaitBeforeReconnect(),
                    new RetryActionsRunner.ExceptionMatcher() {
                        @Override
                        public boolean apply(Exception e) {
                            return e instanceof HttpStatusException
                                    || e instanceof java.util.concurrent.ExecutionException;
                        }
                    });

            loadedVacancies.add(loadVacancyAction.getVacancy());
        }

        return loadedVacancies;
    }

    private static class LoadVacancyAction implements ExceptionThrowingAction {

        private static final Logger LOG = LoggerFactory.getLogger(LoadVacancyAction.class);

        private Vacancy vacancy;

        private Future<Vacancy> task;

        public LoadVacancyAction(Future<Vacancy> task) {
            this.task = task;
        }

        public Vacancy getVacancy() {
            if (vacancy == null) {
                throw new IllegalStateException("Vacancy didn't loaded yet");
            }
            return vacancy;
        }

        @Override
        public void run() throws Exception {
            vacancy = task.get();
        }

        @Override
        public void onIntermediateRetry(Exception e, int currentRetryNumber) {
            LOG.warn("Got an error when loading vacancy info, retrying ... [{}]",
                    currentRetryNumber, e);
        }

        @Override
        public void onLastRetry(Exception e, int currentRetryNumber) {
            throw new RuntimeException("Cannot load vacancy info after "
                    + currentRetryNumber + " retry attempts", e);
        }
    }
}