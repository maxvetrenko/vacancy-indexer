package com.maxvetrenko.vacancy.ui.utils;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public enum SpringContext {

    INSTANCE(new ClassPathXmlApplicationContext("context.xml"));

    private AbstractApplicationContext context;

    private SpringContext(AbstractApplicationContext context) {
        this.context = context;
    }

    public static <T> T getBean(Class<T> clazz) {
        return INSTANCE.getContext().getBean(clazz);
    }

    public AbstractApplicationContext getContext() {
        return context;
    }
}
