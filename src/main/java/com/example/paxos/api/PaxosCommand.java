package com.example.paxos.api;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.core.PaxosCore;
import com.example.paxos.exception.UnKnowPhaseException;
import com.example.paxos.util.ConstansAndUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import java.util.logging.Logger;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
public class PaxosCommand extends BaseServerRequest{

    private static final Logger PAXOS_COMMAND_LOGGER = Logger.getLogger("paxos-command-logger");


    public Mono<ServerResponse> sendProposal(ServerRequest serverRequest) {
        Message message = parser(serverRequest,Message.class);
        if(Message.isEmpty(message)){
            return ServerResponse.badRequest().build();
        }
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
            if(!phase.equals(Phase.PREPARE)){
                return ServerResponse.badRequest().build();
            }
        }catch (Exception e){
            return ServerResponse.badRequest().build();
        }
        PaxosCore.reviceProposalFromProposor(phase,message);
        return ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(Mono.just(new Message.MessageBuilder<>().setCode(200).build()),Message.class);
    }

    public Mono<ServerResponse> replyProposal(ServerRequest serverRequest){
        Message message = parser(serverRequest,Message.class);
        if(Message.isEmpty(message)){
            return ServerResponse.badRequest().build();
        }
        Object proposal = message.getT();
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
        }catch (UnKnowPhaseException e){
            return ServerResponse.badRequest().build();
        }
        if(proposal instanceof Proposal){
            PaxosCore.reviceProposalFromAcceptor(phase,message);
        }
        return ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(Mono.just(new Message.MessageBuilder<>().setCode(200).build()),Message.class);
    }

    public Mono<ServerResponse> approvedSendProposal(ServerRequest serverRequest){
        Message message = parser(serverRequest,Message.class);
        if(Message.isEmpty(message)){
            return ServerResponse.badRequest().build();
        }
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
            if(!phase.equals(Phase.APPROVE)){
                return ServerResponse.badRequest().build();
            }
        }catch (Exception e){
            return ServerResponse.badRequest().build();
        }
        PaxosCore.reviceProposalFromProposor(phase,message);
        return ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(Mono.just(new Message.MessageBuilder<>().setCode(200).build()),Message.class);
    }

    public Mono<ServerResponse> replyChosenValue(ServerRequest serverRequest){
        Message message = parser(serverRequest,Message.class);
        PAXOS_COMMAND_LOGGER.info("choosened value:" + message.toString());
        PaxosCore.stopSendProposal(((Proposal)message.getT()).getContent());
        return ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(Mono.just(new Message.MessageBuilder<>().setCode(200).build()),Message.class);
    }

    public Mono<ServerResponse> learning(ServerRequest serverRequest){
        Message message = parser(serverRequest,Message.class);
        if(Message.isEmpty(message)){
            return ServerResponse.badRequest().build();
        }
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
            if(!phase.equals(Phase.LEARNING)){
                return ServerResponse.badRequest().build();
            }
        }catch (UnKnowPhaseException e){
            return ServerResponse.badRequest().build();
        }
        if(!(message.getT() instanceof  Proposal)){
            return ServerResponse.badRequest().build();
        }
        Proposal proposal = (Proposal) message.getT();
        System.out.println("==========================learning a value" + proposal.toString());
        PaxosCore.learning(proposal);
        return ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(Mono.just(new Message.MessageBuilder<>().setCode(200).build()),Message.class);
    }

    @RequestMapping("/webflux")
    public Mono<String> test(){
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {

        }
        return Mono.just("webflux");
    }

    @RequestMapping("/webClient")
    public Mono<String> webClient(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }
        return Mono.just("webClient");
    }
}