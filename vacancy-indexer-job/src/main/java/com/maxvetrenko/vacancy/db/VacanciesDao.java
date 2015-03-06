package com.maxvetrenko.vacancy.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.maxvetrenko.vacancy.domain.Vacancy;

@Component
public class VacanciesDao {

    private static final String SQL_INSERT_VACANCY =
                "WITH to_be_inserted (URL, NAME, PUBLICATION_DATE, JOB_LOCATION, COMPANY, INDUSTRY, EMPLOYMENT_TYPE)"
                + " AS (VALUES (?, ?, cast(? AS DATE), ?, ?, ?, ?)),"
                + " existing AS ("
                + " SELECT URL FROM to_be_inserted WHERE EXISTS (SELECT 1 FROM VACANCY WHERE URL = to_be_inserted.URL))"
                + " INSERT INTO VACANCY" 
                + " SELECT * FROM  to_be_inserted"
                + " WHERE URL NOT IN (SELECT URL FROM existing)";

    private JdbcTemplate template;

    private TransactionTemplate transactionTemplate;

    @Inject
    public VacanciesDao(DataSource datasource) {
        this.template = new JdbcTemplate(datasource);
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(template.getDataSource());
        transactionTemplate = new TransactionTemplate(txManager);
    }

    public void doInTransaction(final Runnable action) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                action.run();
            }
        });
    }

    public void saveVacanciesToDb(final List<Vacancy> vacancies) {
        template.batchUpdate(SQL_INSERT_VACANCY, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Vacancy vacancy = vacancies.get(i);
                ps.setString(1, vacancy.getUrl());
                ps.setString(2, vacancy.getName());
                ps.setDate(3, vacancy.getPublicationDate());
                ps.setString(4, vacancy.getJobLocation());
                ps.setString(5, vacancy.getCompany());
                ps.setString(6, vacancy.getIndustry());
                ps.setString(7, vacancy.getEmploymentType());
            }

            @Override
            public int getBatchSize() {
                return vacancies.size();
            }
        });
    }

    public void clearVacancyTable() {
        template.update("TRUNCATE VACANCY");
    }

    public void updateFullText() {
        template.update("UPDATE VACANCY SET FULL_TEXT = URL || ' ' || NAME || ' ' || "
                + "PUBLICATION_DATE || ' ' || JOB_LOCATION || ' ' || COMPANY || ' ' ||"
                + "INDUSTRY || ' ' || EMPLOYMENT_TYPE");
    }
}
