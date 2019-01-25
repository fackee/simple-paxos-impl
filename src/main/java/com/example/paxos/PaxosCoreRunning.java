package com.example.paxos;

import com.example.paxos.core.PaxosCore;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PaxosCoreRunning implements ApplicationListener<EmbeddedServletContainerInitializedEvent>{


    @Override
    public void onApplicationEvent(EmbeddedServletContainerInitializedEvent applicationStartingEvent) {
        PaxosCore.init();
    }
}