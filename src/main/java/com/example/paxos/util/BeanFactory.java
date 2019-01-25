package com.example.paxos.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author zhujianxin
 * @date 2019/1/25.
 */
@Component
public class BeanFactory implements ApplicationContextAware {


    private static ApplicationContext applicationContext;


    public static ApplicationContext getApplicationContext() {
        return BeanFactory.applicationContext;
    }


    public static <T> T getBean(Class<T> tClass){
        return getApplicationContext().getBean(tClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BeanFactory.applicationContext = applicationContext;
    }
}
