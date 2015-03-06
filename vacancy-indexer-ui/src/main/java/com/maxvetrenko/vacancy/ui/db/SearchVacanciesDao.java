package com.maxvetrenko.vacancy.ui.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.maxvetrenko.vacancy.domain.Vacancy;

@Component
public class SearchVacanciesDao {

    private static final String SQL_FULL_TEXT_SEARCH = "SELECT * FROM vacancy WHERE FULL_TEXT LIKE ''{0}''";

    private static final String SQL_LATEST_VACANCIES = "SELECT * FROM ( SELECT * FROM vacancy ORDER BY publication_date desc ) AS derivedTable";

    private JdbcTemplate template;

    @Inject
    public void setDataSource(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public List<Vacancy> searchVacancies(String searchString) {
        String sql = MessageFormat.format(SQL_FULL_TEXT_SEARCH, "%" + searchString + "%");
        return template.query(sql, new VacancyMapper());
    }

    public List<Vacancy> getLatestVacancies(int resultsCount) {
        return template.query(MessageFormat.format(SQL_LATEST_VACANCIES, resultsCount), new VacancyMapper());
    }

    private static final class VacancyMapper implements RowMapper<Vacancy> {
        @Override
        public Vacancy mapRow(ResultSet rs, int rpwNumber) throws SQLException {
            Vacancy vacancy = new Vacancy(rs.getString("URL"), rs.getString("NAME"));
            vacancy.setPublicationDate(rs.getDate("PUBLICATION_DATE"));
            vacancy.setJobLocation(rs.getString("JOB_LOCATION"));
            vacancy.setCompany(rs.getString("COMPANY"));
            vacancy.setIndustry(rs.getString("INDUSTRY"));
            vacancy.setEmploymentType(rs.getString("EMPLOYMENT_TYPE"));
            return vacancy;
        }
    }
}
