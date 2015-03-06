package com.maxvetrenko.vacancy.ui;

import com.maxvetrenko.vacancy.ui.utils.SpringContext;

public class Runner {

    public static void main(String[] args) {
        SpringContext.getBean(VacancySearchWindow.class).open();
    }
}
