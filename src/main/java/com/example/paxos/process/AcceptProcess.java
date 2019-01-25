package com.example.paxos.process;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.AcceptedStatus;
import com.example.paxos.bean.paxos.Node;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.exception.MessageUnsupportException;
import com.example.paxos.proxy.NodeProxy;
import com.example.paxos.util.BeanFactory;
import com.example.paxos.util.ConstansAndUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class AcceptProcess implements Runnable {

    private static final Logger ACCEPT_PROCESS_LOGGER = Logger.getLogger("ACCEPT_LOGGER");

    private static final AcceptedStatus CURRENT_ACCEPTED_STATUS = new AcceptedStatus();

    private static final LinkedBlockingQueue<Pair> ACCEPTED_REQUESTS =
            new LinkedBlockingQueue<>();

    final AsyncRestTemplate asyncRestTemplate = BeanFactory.getBean(AsyncRestTemplate.class);

    public AcceptProcess() {
    }


    public static void addRequest(Pair<Phase, Message> action) {
        Phase phase = action.getKey();
        Message message = action.getValue();
        if (!(message.getT() instanceof Proposal)) {
            ACCEPT_PROCESS_LOGGER.warning("accpted message is illegal:" + message.getT().getClass().getSimpleName());
            throw new MessageUnsupportException(Thread.currentThread().getName() + "-" +
                    "accpted process:message is illegal:" + message.getT().getClass().getSimpleName());
        }
        Proposal proposal = (Proposal) message.getT();
        // 角色判断
        NodeProxy nodeProxy = NodeProxy.NodeProxyInstance.INSTANCE.getInstance();
        if (!nodeProxy.isProposor(proposal.getVoteFrom())) {
            ACCEPT_PROCESS_LOGGER.warning("the vote message isn't from a proposal");
            return;
        }
        ACCEPTED_REQUESTS.add(action);
    }

    @Override
    public void run() {

        Node local = NodeProxy.NodeProxyInstance.INSTANCE.getInstance().getLocalServer();
        //current role must be a acceptor
        if (!local.getRole().isAcceptor()) {
            //TODO shutdown the scheduler
            return;
        }

        while (!ACCEPTED_REQUESTS.isEmpty()) {
            try {
                Pair action = ACCEPTED_REQUESTS.take();
                Phase phase = (Phase) action.getKey();
                Message message = (Message) action.getValue();
                Proposal proposal = (Proposal) message.getT();
                //PREPARE 阶段处理逻辑
                if (phase.equals(Phase.PREPARE)) {
                    HttpEntity httpEntity = HttpEntity.EMPTY;
                    Message<Proposal> replyMessage = new Message<Proposal>(proposal);
                    // 1. 已经chosen，则返回chosen-value
                    if (CURRENT_ACCEPTED_STATUS.hasChosendValue()) {
                        replyMessage.setMessage(Phase.APPROVE.getPhase());
                        replyMessage.setCode(Phase.APPROVE.getCode());
                        Proposal replyProposal = new Proposal();
                        replyProposal.setContent(CURRENT_ACCEPTED_STATUS.getChosenValue());
                        replyProposal.setVoteFrom(local.getIp());
                        replyProposal.setHasChoosen(true);
                        httpEntity = new HttpEntity<>(replyMessage);
                    }
                    //2. check lastAccept[number，value]，如果为空，更新lastAccept，即必须接受第一个提案
                    if (CURRENT_ACCEPTED_STATUS.isFirstAcceptProposal()) {
                        updateCurrentAcceptedStatus(proposal.getContent(), proposal.getNumber(), false);
                        replyMessage.setMessage(Phase.PREPARE.getPhase());
                        replyMessage.setCode(Phase.PREPARE.getCode());
                        Proposal replyProposal = new Proposal();
                        BeanUtils.copyProperties(proposal, replyProposal);
                        replyProposal.setVoteFrom(local.getIp());
                        replyMessage.setT(replyProposal);
                        httpEntity = new HttpEntity<>(replyMessage);
                    }
                    //2. check lastAccept，如果不为空，返回上次accept的提案。
                    if (CURRENT_ACCEPTED_STATUS.getLastAcceptedValue() != null) {
                        //check number，如果大于lastAcceptedNumber，则更新number为更大的值
                        if (CURRENT_ACCEPTED_STATUS.greaterThanLastProposalNumber(proposal.getNumber())) {
                            updateCurrentAcceptedStatus(null, proposal.getNumber(), false);
                        }

                    }
                    if (CURRENT_ACCEPTED_STATUS.greaterThanLastProposalNumber(proposal.getNumber())) {
                        //更新提案number，返回上次接受的提案
                        updateCurrentAcceptedStatus(null, proposal.getNumber(), false);
                        replyMessage.setMessage(Phase.PREPARE.getPhase());
                        replyMessage.setCode(Phase.PREPARE.getCode());
                        Proposal replyProposal = new Proposal();
                        replyProposal.setContent(CURRENT_ACCEPTED_STATUS.getLastAcceptedValue());
                        replyProposal.setNumber(CURRENT_ACCEPTED_STATUS.getLastAcceptedNumber());
                        replyProposal.setVoteFrom(local.getIp());
                        httpEntity = new HttpEntity<>(replyMessage);
                    }
                    asyncRestTemplate.postForEntity(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.PORT + ConstansAndUtils.API_COMMAND_PREPARE_REPLY_PROPOSAL,
                            httpEntity,Message.class)
                            .addCallback((success)->{
                                ACCEPT_PROCESS_LOGGER.info("reply proposal to proposor success:" + success.getBody().toString());
                            },(error)->{
                                ACCEPT_PROCESS_LOGGER.info("reply proposal to proposor fial:" + error.getMessage());
                            });

                }
                //APPROVE 阶段处理逻辑
                if (phase.equals(Phase.APPROVE)) {
                    //批准提案并返回
                    if (CURRENT_ACCEPTED_STATUS.hasChosendValue() && !CURRENT_ACCEPTED_STATUS.getChosenValue().equals(proposal.getContent())) {
                        ACCEPT_PROCESS_LOGGER.warning("!!!!!!!!!!!!!!!!!!!!!!!!the value not consistant!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    }
                    updateCurrentAcceptedStatus(proposal.getContent(),proposal.getNumber(),true);
                    //返回批准的提案
                    proposal.setVoteFrom(local.getIp());
                    HttpEntity<Message> approvedEntity = new HttpEntity<>(new Message.MessageBuilder<Proposal>()
                            .setT(proposal)
                            .setCode(Phase.APPROVE.getCode())
                            .setMsg(Phase.APPROVE.getPhase()).build());
                    asyncRestTemplate.postForEntity(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.PORT + ConstansAndUtils.API_COMMAND_APPROVED_REPLY_CHOSENED_VALUE,
                            approvedEntity,Message.class)
                            .addCallback((success)->{
                                ACCEPT_PROCESS_LOGGER.info("send proposal to acceptors success:" + success.getBody().toString());
                            },(error)->{
                                ACCEPT_PROCESS_LOGGER.info("send proposal to acceptors fial:" + error.getMessage());
                            });



                    //通知其中一个learner学习
                    proposal.setVoteFrom(local.getIp());
                    HttpEntity<Message> learnerEntity = new HttpEntity<>(new Message.MessageBuilder<Proposal>()
                            .setT(proposal)
                            .setCode(Phase.LEARNING.getCode())
                            .setMsg(Phase.LEARNING.getPhase()).build());

                    asyncRestTemplate.postForEntity(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.PORT + ConstansAndUtils.API_COMMAND_APPROVED_LEARNING,
                            learnerEntity,Message.class)
                            .addCallback((success)->{
                                ACCEPT_PROCESS_LOGGER.info("send proposal to acceptors success:" + success.getBody().toString());
                            },(error)->{
                                ACCEPT_PROCESS_LOGGER.info("send proposal to acceptors fail:" + error.getMessage());
                            });
                }
            } catch (InterruptedException e) {
                ACCEPT_PROCESS_LOGGER.warning("take accepted request from queue exception" + e.getMessage());
            }
        }
    }

    private void updateCurrentAcceptedStatus(String value, Integer number, boolean choosen) {
        if (value != null) {
            CURRENT_ACCEPTED_STATUS.setLastAcceptedValue(value);
        }
        if (number != null) {
            CURRENT_ACCEPTED_STATUS.setLastAcceptedNumber(number);
        }
        if (choosen) {
            CURRENT_ACCEPTED_STATUS.setChosenValue(value);
        }
    }
}