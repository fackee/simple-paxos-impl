package com.example.paxos.util.thread;

import java.util.concurrent.ThreadLocalRandom;

public class RandomTimeout {

    private static final ThreadLocalRandom THREAD_LOCAL_RANDOM = ThreadLocalRandom.current();

    private static final Long RANDOM_SEED = 1000L;

    public static synchronized void timeout() throws InterruptedException{
        Thread.sleep(THREAD_LOCAL_RANDOM.nextLong(RANDOM_SEED));
    }

}