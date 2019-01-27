package com.example.paxos.core;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.Node;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.process.AcceptProcess;
import com.example.paxos.process.ProposalProcess;
import com.example.paxos.proxy.NodeProxy;
import com.example.paxos.util.BeanFactory;
import com.example.paxos.util.thread.Executors;
import com.example.paxos.util.ConstansAndUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.AsyncRestTemplate;
import java.util.logging.Logger;

public class PaxosCore {

    private static final Logger PAXOS_CORE_LOGGER = Logger.getLogger(PaxosCore.class.getSimpleName());

    public static volatile boolean isInit = false;

    private static final AsyncRestTemplate asyncRestTemplate = BeanFactory.getBean(AsyncRestTemplate.class);

    public static void init(){
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
        if(proposal.isHasChoosen() != null && proposal.isHasChoosen()){
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
        proposal.setVoteFrom(local.getIp());
        HttpEntity<Message> httpEntity = new HttpEntity<>(new Message.MessageBuilder<Proposal>()
                .setT(proposal)
                .setCode(Phase.APPROVE.getCode())
                .setMsg(Phase.APPROVE.getPhase())
                .build());
        asyncRestTemplate.postForEntity(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.PORT + ConstansAndUtils.API_COMMAND_APPROVED_SEND_PROPOSAL,
                httpEntity,Message.class)
                .addCallback((success)->{
                    PAXOS_CORE_LOGGER.info("APPROVED: send proposal to acceptors success in approved:");
                },(error)->{
                    PAXOS_CORE_LOGGER.info("APPROVED: send proposal to acceptors fail in approved:");
                });
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
        NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getAllLearner().parallelStream().forEach( learnerNode ->{
            if(!learnerNode.getIp().equals(NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getLocalServer().getIp())){
                HttpEntity<Message> httpEntity = new HttpEntity<>(new Message.MessageBuilder<Proposal>()
                        .setT(proposal)
                        .setCode(Phase.LEARNING.getCode())
                        .setMsg(Phase.LEARNING.getPhase()).build());
                asyncRestTemplate.postForEntity(ConstansAndUtils.HTTP_PREFIXX + learnerNode.getIp() + ConstansAndUtils.PORT + ConstansAndUtils.API_COMMAND_APPROVED_LEARNING,
                        httpEntity,Message.class)
                        .addCallback((success)->{
                            PAXOS_CORE_LOGGER.info("send learning message to other learner success:");
                        },(error)->{
                            PAXOS_CORE_LOGGER.info("send learning message to other learner fail:");
                        });
            }
        });
    }

    private static boolean loadClusterInfo(){
        return PaxosStore.load();
    }

}