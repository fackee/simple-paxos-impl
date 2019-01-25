package com.example.paxos.core;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.Node;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.process.AcceptProcess;
import com.example.paxos.process.ProposalProcess;
import com.example.paxos.proxy.NodeProxy;
import com.example.paxos.util.thread.Executors;
import com.example.paxos.util.ConstansAndUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class PaxosCore {

    private static final Logger PAXOS_CORE_LOGGER = Logger.getLogger(PaxosCore.class.getSimpleName());

    public static volatile boolean isInit = false;


    public static void init(){
        System.out.println("====================starting paxos server==================");
        if(loadClusterInfo()){
            isInit = true;
        }
        ProposalProcess.needSendProposal = true;
        Executors.electionSchedule();
        Executors.acceptSchedule();
    }


    public static void reviceProposalFromProposor(Phase phase,Message message){
        Pair<Phase,Message> action = Pair.of(phase,message);
        AcceptProcess.addRequest(action);
    }

    public static void reviceProposalFromAcceptor(Phase phase,Message message){
        Node local = NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getLocalServer();

        if(!local.getRole().isProposer()){
            PAXOS_CORE_LOGGER.warning("");
            return;
        }
        Proposal proposal = (Proposal) message.getT();
        if(!local.getIp().equals(proposal.getVoteFrom())){
            PAXOS_CORE_LOGGER.warning("");
            return;
        }
        //选票逻辑判断
        //阶段校验
        if(!phase.equals(Phase.PREPARE)){
            return;
        }
        //已经选出value,不处理
        if(proposal.isHasChoosen()){
            return;
        }
        //value不同,更新value
        if(!proposal.getContent().equals(ProposalProcess.getCurrentProposedStatus().getLastSendedValue())){
            ProposalProcess.getCurrentProposedStatus().setLastSendedValue(proposal.getContent());
        }
        //number更大,更新number
        if(proposal.getNumber() > ProposalProcess.getCurrentProposedStatus().getLastSendedNumber()){
            ProposalProcess.getCurrentProposedStatus().setLastSendedNumber(proposal.getNumber());
        }
        WebClient webClient = WebClient.create(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.API_COMMAND_APPROVED_SEND_PROPOSAL);
        proposal.setVoteFrom(local.getIp());
        webClient.post()
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(new Message.MessageBuilder<Proposal>()
                        .setT(proposal)
                        .setCode(Phase.APPROVE.getCode())
                        .setMsg(Phase.APPROVE.getPhase()))
                .retrieve()
                .bodyToMono(Message.class)
                .doOnSuccess(message1 -> {})
                .doOnError((throwable -> {}))
                .subscribe();
    }

    public static void stopSendProposal(String choosenValue){
        ProposalProcess.needSendProposal = false;
        NodeProxy.NodeProxyInstance.INSTANCE.getInstance().choosenedValue(choosenValue);
    }

    public static void learning(Proposal proposal){
        Node local = NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getLocalServer();
        if(!local.getRole().isLearner()){
            PAXOS_CORE_LOGGER.warning("current role isn't a learner,can't learning");
            return;
        }
        PaxosStore.learning(proposal);
        WebClient webClient = WebClient.create();
        NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getAllLearner().parallelStream().forEach( learnerNode ->{
            webClient.mutate().baseUrl(ConstansAndUtils.HTTP_PREFIXX + learnerNode.getIp() + ConstansAndUtils.API_COMMAND_APPROVED_LEARNING);
            webClient.post()
                    .accept(MediaType.APPLICATION_JSON_UTF8)
                    .syncBody(new Message.MessageBuilder<Proposal>()
                            .setT(proposal)
                            .setCode(Phase.LEARNING.getCode())
                            .setMsg(Phase.LEARNING.getPhase()))
                    .retrieve()
                    .bodyToMono(Message.class)
                    .doOnSuccess(message1 -> {})
                    .doOnError((throwable -> {}))
                    .subscribe();
        });
    }

    private static boolean loadClusterInfo(){
        return PaxosStore.load();
    }

}