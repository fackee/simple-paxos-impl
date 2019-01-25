package com.example.paxos.util.thread;

import com.example.paxos.process.AcceptProcess;
import com.example.paxos.process.HeartBeatProcess;
import com.example.paxos.process.ProposalProcess;
import com.example.paxos.util.ConstansAndUtils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Executors {

    private static final ScheduledExecutorService PROPOSAL_EXE = java.util.concurrent.Executors.newSingleThreadScheduledExecutor((runnable)->{
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setName("paxos proposal process thread");
        return thread;
    });

    private static final ScheduledExecutorService ACCEPT_EXE = java.util.concurrent.Executors.newSingleThreadScheduledExecutor((runnable)->{
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setName("paxos accept process thread");
        return thread;
    });

    private static final ScheduledExecutorService hearbeat = java.util.concurrent.Executors.newSingleThreadScheduledExecutor((runnable)->{
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setName("paxos learning process thread");
        return thread;
    });

    public static void electionSchedule(){
        PROPOSAL_EXE.scheduleAtFixedRate(new ProposalProcess(),
                ConstansAndUtils.PROCESS_SCHEDILE_INIT_DELAY,
                ConstansAndUtils.PROCESS_SCHEDILE_PERIOD,
                TimeUnit.MILLISECONDS);
    }

    public static void acceptSchedule(){
        ACCEPT_EXE.scheduleAtFixedRate(new AcceptProcess(),
                ConstansAndUtils.PROCESS_SCHEDILE_INIT_DELAY,
                ConstansAndUtils.PROCESS_SCHEDILE_PERIOD,
                TimeUnit.MILLISECONDS);
    }

    public static void hearbeatSchedule(){
        PROPOSAL_EXE.scheduleAtFixedRate(new HeartBeatProcess(),
                ConstansAndUtils.PROCESS_SCHEDILE_INIT_DELAY,
                ConstansAndUtils.PROCESS_SCHEDILE_PERIOD,
                TimeUnit.MILLISECONDS);
    }
}