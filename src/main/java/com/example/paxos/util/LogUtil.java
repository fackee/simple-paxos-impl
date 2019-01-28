package com.example.paxos.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhujianxin
 * @date 2019/1/28.
 */
public class LogUtil {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger("DEFAULT-LOG");

    private static final Logger LEARNING_LOGGER = LoggerFactory.getLogger("LEARNING-LOG");


    public static final void info(String format,String... args){
        DEFAULT_LOGGER.info(format,args);
    }

    public static final void error(String format,String... args){
        DEFAULT_LOGGER.error(format,args);
    }

    public static final void learning(String format,String... args){
        LEARNING_LOGGER.error(format,args);
    }

}
