package com.maxvetrenko.vacancy;

import com.maxvetrenko.vacancy.utils.SpringContext;

public final class Runner {

    private Runner() {}

    public static void main(String[] args) {
        VacanciesSaver crawler = SpringContext.getBean(VacanciesSaver.class);
        crawler.run();
    }
}
