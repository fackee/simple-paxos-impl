package com.test.load;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.util.ConstansAndUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Web {

    public static void main(String[] args) throws InterruptedException {
        new Thread(()->{
            WebClient webClient = WebClient.create();
            webClient.get()
                    .uri(URI.create("http://127.0.0.1/v1/paxos/webflux"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess((res) -> {
                        System.out.println("=================webfluxdoOnSuccess");
                    })
                    .doOnError((t) -> {
                        System.out.println("=========================doOnError" + t.getMessage());
                    }).subscribe();
        }).start();
    }
}