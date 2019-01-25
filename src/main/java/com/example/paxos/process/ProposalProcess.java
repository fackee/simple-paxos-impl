package com.example.paxos.process;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.*;
import com.example.paxos.exception.OperatorUnSupportException;
import com.example.paxos.proxy.NodeProxy;
import com.example.paxos.util.ConstansAndUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ProposalProcess implements Runnable{

    private static final AtomicInteger numberCounter = new AtomicInteger(0);

    private static final Logger PROPOSAL_PROCESS_LOGGER = Logger.getLogger("proposal-process");

    private static final ProposedStatus CURRENT_PROPOSED_STATUS = new ProposedStatus();

    public static volatile boolean needSendProposal = false;

    public ProposalProcess(){}

    @Override
    public void run() {
        if(!needSendProposal){
            PROPOSAL_PROCESS_LOGGER.info("don't need send proposal now");
            return;
        }
        final NodeProxy nodeProxy = NodeProxy.NodeProxyInstance.INSTANCE.getInstance();
        //current node is a proposor
        if(!nodeProxy.getLocalServer().getRole().isProposer()){
            throw new OperatorUnSupportException("this role:" + Arrays.toString(nodeProxy.getLocalServer().getRole().getRoleTypes().toArray()) + "cannot proposal vote");
        }
        Proposal proposal = new Proposal();
        // acceptors hase choosened a value
        if(!StringUtils.isEmpty(nodeProxy.choosenedValue())){
            proposal.setContent(nodeProxy.choosenedValue());
        }
        //global increment number
        proposal.setNumber(nodeProxy.getLocalServer().getNodeNumber() + numberCounter.incrementAndGet());
        //the single value in cluter is ip,choosen the value like electron a leader
        proposal.setContent(nodeProxy.getLocalServer().getIp());
        proposal.setVoteFrom(nodeProxy.getLocalServer().getIp());
        //parallel send proposal to majority acceptors
        CURRENT_PROPOSED_STATUS.setLastSendedNumber(proposal.getNumber());
        CURRENT_PROPOSED_STATUS.setLastSendedValue(proposal.getContent());
        nodeProxy.getMojorityAcceptors().parallelStream().forEach( acceptor -> {
            WebClient webClient = WebClient.create(ConstansAndUtils.HTTP_PREFIXX + acceptor.getIp() + ConstansAndUtils.API_COMMAND_PREPARE_SEND_PROPOSAL);
            webClient.post()
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .syncBody(new Message.MessageBuilder<Proposal>()
                            .setCode(Phase.PREPARE.getCode())
                            .setMsg(Phase.PREPARE.getPhase())
                            .setT(proposal).build())
                    .retrieve()
                    .bodyToMono(Message.class)
                    .doOnSuccess(message1 -> {
                        System.out.println("==================doOnSuccess" + message1);
                    })
                    .doOnError((throwable -> {
                        System.out.println("==================doOnError" + throwable);
                    })).subscribe();
        });
    }


    private Integer currentProposalNumber(){
        return NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getLocalServer().getNodeNumber() + numberCounter.get();
    }

    public static ProposedStatus getCurrentProposedStatus() {
        return CURRENT_PROPOSED_STATUS;
    }
}