package com.example.paxos;

import com.example.paxos.core.PaxosCore;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PaxosCoreRunning implements ApplicationListener<WebServerInitializedEvent>{
    @Override
    public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        PaxosCore.init();
    }
}