package com.example.paxos.core;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.Node;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.process.AcceptProcess;
import com.example.paxos.process.ProposalProcess;
import com.example.paxos.proxy.NodeProxy;
import com.example.paxos.util.BeanFactory;
import com.example.paxos.util.LogUtil;
import com.example.paxos.util.thread.Executors;
import com.example.paxos.util.ConstansAndUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PaxosCore {

    public static volatile boolean isInit = false;

    private static final AsyncRestTemplate asyncRestTemplate = BeanFactory.getBean(AsyncRestTemplate.class);

    private static AtomicInteger RECIVED_PROPOSAL_FROM_ACCEPTOR = new AtomicInteger(0);

    public static void init(){
        if(loadClusterInfo()){
            isInit = true;
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        NodeProxy nodeProxy = NodeProxy.NodeProxyInstance.INSTANCE.getInstance();
        Node local = nodeProxy.getLocalServer();

        if(!local.getRole().isProposer()){
            LogUtil.info("role error");
            return;
        }
        Proposal proposal = (Proposal) message.getT();
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
        if(RECIVED_PROPOSAL_FROM_ACCEPTOR.incrementAndGet() >= nodeProxy.getMojorityAcceptors().size()){
            LogUtil.info("get reply proposal form acceptor more than majority");
            HttpEntity<Message> httpEntity = new HttpEntity<>(new Message.MessageBuilder<Proposal>()
                    .setT(proposal)
                    .setCode(Phase.APPROVE.getCode())
                    .setMsg(Phase.APPROVE.getPhase())
                    .build());
            asyncRestTemplate.postForEntity(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.PORT + ConstansAndUtils.API_COMMAND_APPROVED_SEND_PROPOSAL,
                    httpEntity,Message.class)
                    .addCallback((success)->{
                        LogUtil.info("APPROVED: send proposal to acceptors success");
                    },(error)->{
                        LogUtil.error("APPROVED: send proposal to acceptors fail");
                    });
        }
    }

    public static void stopSendProposal(String choosenValue){
        ProposalProcess.needSendProposal = false;
        NodeProxy.NodeProxyInstance.INSTANCE.getInstance().choosenedValue(choosenValue);
    }

    public static void learning(Proposal proposal){
        Node local = NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getLocalServer();
        if(!local.getRole().isLearner()){
            LogUtil.error("current role isn't a learner,can't learning");
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
                            LogUtil.info("send learning message to other learner success");
                        },(error)->{
                            LogUtil.error("send learning message to other learner fail");
                        });
            }
        });
    }

    private static boolean loadClusterInfo(){
        return PaxosStore.load();
    }

}