package com.maxvetrenko.vacancy.ui;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.maxvetrenko.vacancy.domain.Vacancy;
import com.maxvetrenko.vacancy.ui.db.SearchVacanciesDao;

@Component
public class VacancySearchWindow {

    private static final String KEY_ORDERING = "ordering";

    private static final String COLUMN_TITLE_PUBLICATION_DATE = "Publication date";

    private static final String COLUMN_TITLE_NAME = "Name";

    private static final String COLUMN_TITLE_JOB_LOCATION = "Job Location";

    private static final String COLUMN_TITLE_COMPANY = "Company";

    private static final String COLUMN_TITLE_INDUSTRY = "industry";

    private static final String COLUMN_TITLE_EMPLOYMENT_TYPE = "Employment type";
    
    private static final String COLUMN_TITLE_URL = "Url";


    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd (HH:MM:SS)");

    private static Table table;

    private List<Vacancy> vacancies = Lists.newArrayList();

    private SearchVacanciesDao searchVacanciesDao;

    @Autowired
    public void setSearchVacanciesDao(SearchVacanciesDao searchVacanciesDao) {
        this.searchVacanciesDao = searchVacanciesDao;
    }

    public void open() {

        Display display = Display.getDefault();

        Shell shell = new Shell();
        shell.setMinimumSize(new Point(140, 90));
        shell.setSize(650, 600);
        shell.setText("Vacancies search");
        shell.setLayout(new FillLayout(SWT.HORIZONTAL));

        createContents(shell);

        Menu menuBar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menuBar);

        MenuItem menuItemHelp = new MenuItem(menuBar, SWT.NONE);
        menuItemHelp.setText("Help");
        // Incomplete ...

        updateTable(searchVacanciesDao.getLatestVacancies(50));

        shell.open();
        shell.layout();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    protected void createContents(Shell shell) {

        Composite mainComposite = new Composite(shell, SWT.NONE);
        mainComposite.setLayout(new GridLayout(1, false));

        Composite upperPart = new Composite(mainComposite, SWT.BORDER);
        upperPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        upperPart.setLayout(new FillLayout(SWT.HORIZONTAL));

        Composite composite = new Composite(upperPart, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        final Text searchTextField = new Text(composite, SWT.BORDER | SWT.SEARCH | SWT.CANCEL);
        GridData gdDataText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdDataText.minimumWidth = 250;
        searchTextField.setLayoutData(gdDataText);

        searchTextField.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {

                searchForVacancies(searchTextField.getText());

                if (event.detail == SWT.CANCEL) {
                    searchTextField.setText("");
                }
            }
        });

        Button searchButton = new Button(composite, SWT.NONE);
        GridData searchButtonGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        searchButton.setLayoutData(searchButtonGridData);
        searchButton.setText("Search");
        searchButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                searchForVacancies(searchTextField.getText());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        ScrolledComposite tableScrollOverlay = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL);
        tableScrollOverlay.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tableScrollOverlay.setExpandVertical(true);
        tableScrollOverlay.setExpandHorizontal(true);

        table = new Table(tableScrollOverlay, SWT.BORDER | SWT.FULL_SELECTION);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        tableScrollOverlay.setContent(table);
        tableScrollOverlay.setMinSize(table.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Listener sortListener = new SortVacancyBySingleColumnListener();

        final TableColumn nameColumn = createTableColumn(table, COLUMN_TITLE_NAME, 200, sortListener);
        TableColumn publicationDateColumn = createTableColumn(table, COLUMN_TITLE_PUBLICATION_DATE, 100, sortListener);
        TableColumn jobLocation = createTableColumn(table, COLUMN_TITLE_JOB_LOCATION, 200, sortListener);
        TableColumn company = createTableColumn(table, COLUMN_TITLE_COMPANY,
                250, sortListener);
        TableColumn industry = createTableColumn(table, COLUMN_TITLE_INDUSTRY, 150, sortListener);
        TableColumn employmentType = createTableColumn(table, COLUMN_TITLE_EMPLOYMENT_TYPE, 150, sortListener);
        TableColumn urlType = createTableColumn(table, COLUMN_TITLE_URL, 150, sortListener);
    }

    private void searchForVacancies(String searchText) {
        this.vacancies = searchVacanciesDao.searchVacancies(searchText);
        updateTable();
    }

    private void updateTable(List<Vacancy> vacancies) {
        this.vacancies = vacancies;
        updateTable();
    }

    private void updateTable() {
        table.removeAll();
        for (Vacancy vacancy : vacancies) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setData(vacancy);

            String[] text = new String[] {
                    vacancy.getName(),
                    format.format(vacancy.getPublicationDate()),
                    vacancy.getJobLocation(),
                    vacancy.getCompany(),
                    vacancy.getIndustry(),
                    vacancy.getEmploymentType(),
                    vacancy.getUrl()
            };

            item.setText(text);
        }
    }

    private static TableColumn createTableColumn(Table table, String title, int width,
            Listener... selectionListeners) {

        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(title);
        column.setWidth(width);
        column.setResizable(true);
        column.setMoveable(true);

        for (Listener listener : selectionListeners) {
            column.addListener(SWT.Selection, listener);
        }

        return column;
    }

    private final class SortVacancyBySingleColumnListener implements Listener {

        public void handleEvent(Event event) {

            if (vacancies == null || vacancies.size() <= 1 || table.getItems() == null
                    || table.getItems().length <= 1) {
                return;
            }

            TableColumn column = (TableColumn) event.widget;

            Object columnOrdering = column.getData(KEY_ORDERING);
            if (columnOrdering == null) {
                sort(column, VacancyComparator.Ordering.DIRECT);
            } else if (columnOrdering instanceof VacancyComparator.Ordering) {
                VacancyComparator.Ordering ordering = (VacancyComparator.Ordering) columnOrdering;
                sort(column, ordering.reverse());
            }

            updateTable();
        }

        private void sort(TableColumn column, VacancyComparator.Ordering ordering) {
            Collections.sort(vacancies, new VacancyComparator(column.getText(), ordering));
            column.setData(KEY_ORDERING, ordering);
        }
    }

    private static class VacancyComparator implements Comparator<Vacancy> {

        public enum Ordering {
            DIRECT,
            REVERSE;

            public Ordering reverse() {
                if (this == DIRECT) {
                    return REVERSE;
                } else {
                    return DIRECT;
                }
            }
        }

        private String columnName;
        private Ordering ordering;

        public VacancyComparator(String columnName, Ordering ordering) {
            this.columnName = columnName;
            this.ordering = ordering;
        }

        @Override
        public int compare(Vacancy o1, Vacancy o2) {

            int result;

            switch (columnName) {
            case COLUMN_TITLE_NAME:
                result = compareStrings(o1.getName(), o2.getName());
                break;
            case COLUMN_TITLE_PUBLICATION_DATE:
                result = compareDates(o1.getPublicationDate(), o2.getPublicationDate());
                break;
            case COLUMN_TITLE_COMPANY:
                result = compareStrings(o1.getCompany(), o2.getCompany());
                break;
            case COLUMN_TITLE_JOB_LOCATION:
                result = compareStrings(o1.getJobLocation(), o2.getJobLocation());
                break;
            case COLUMN_TITLE_INDUSTRY:
                result = compareStrings(o1.getIndustry(), o2.getIndustry());
                break;
            case COLUMN_TITLE_EMPLOYMENT_TYPE:
                result = compareStrings(o1.getEmploymentType(), o2.getEmploymentType());
                break;
            default:
                throw new IllegalArgumentException("Unknown column name: " + columnName);
            }

            return ordering == Ordering.REVERSE ? result * -1 : result;
        }

        public int compareStrings(String str1, String str2) {
            return str1.compareTo(str2);
        }

        public int compareDates(Date date1, Date date2) {
            return date1.compareTo(date2);
        }
    };

}
