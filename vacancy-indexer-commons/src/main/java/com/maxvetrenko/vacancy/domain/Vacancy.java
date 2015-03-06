package com.maxvetrenko.vacancy.domain;

import java.sql.Date;

public class Vacancy {

    private final String url;

    private final String name;
    private Date publicationDate;

    private String jobLocation;
    private String company;
    private String industry;
    private String employmentType;

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Vacancy(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(String jobLocation) {
        this.jobLocation = jobLocation;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }
    
    @Override
    public String toString() {
        return "Vacancy [" + "URL= " + url + " ,name= " + getName() + " ,publicationDate= " + getPublicationDate()
                + " ,jobLocation= " + getJobLocation() + " ,company= " + getCompany()
                + " ,employmentType= " + employmentType + "]";
    }
}
