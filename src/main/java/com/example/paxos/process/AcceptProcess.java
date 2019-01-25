package com.example.paxos.process;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.AcceptedStatus;
import com.example.paxos.bean.paxos.Node;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.exception.MessageUnsupportException;
import com.example.paxos.proxy.NodeProxy;
import com.example.paxos.util.ConstansAndUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class AcceptProcess implements Runnable {

    private static final Logger ACCEPT_PROCESS_LOGGER = Logger.getLogger("ACCEPT_LOGGER");

    private static final AcceptedStatus CURRENT_ACCEPTED_STATUS = new AcceptedStatus();

    private static final LinkedBlockingQueue<Pair> ACCEPTED_REQUESTS =
            new LinkedBlockingQueue<>();

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
                WebClient webClient = null;
                //PREPARE 阶段处理逻辑
                if (phase.equals(Phase.PREPARE)) {
                    webClient = WebClient.create(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.API_COMMAND_PREPARE_REPLY_PROPOSAL);
                    Message<Proposal> replyMessage = new Message<Proposal>(proposal);
                    // 1. 已经chosen，则返回chosen-value
                    if (CURRENT_ACCEPTED_STATUS.hasChosendValue()) {
                        replyMessage.setMessage(Phase.APPROVE.getPhase());
                        replyMessage.setCode(Phase.APPROVE.getCode());
                        Proposal replyProposal = new Proposal();
                        replyProposal.setContent(CURRENT_ACCEPTED_STATUS.getChosenValue());
                        replyProposal.setVoteFrom(local.getIp());
                        replyProposal.setHasChoosen(true);
                        webClient.post().accept(MediaType.APPLICATION_JSON_UTF8).syncBody(replyMessage)
                                .retrieve()
                                .bodyToMono(Message.class)
                                .doOnSuccess(message1 -> {})
                                .doOnError((throwable -> {}))
                                .subscribe();
                        continue;
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
                        webClient.post().accept(MediaType.APPLICATION_JSON_UTF8).syncBody(new Message(proposal))
                                .retrieve()
                                .bodyToMono(Message.class)
                                .doOnSuccess(message1 -> {})
                                .doOnError((throwable -> {}))
                                .subscribe();
                        continue;
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
                        webClient.post().accept(MediaType.APPLICATION_JSON_UTF8).syncBody(replyMessage)
                                .retrieve()
                                .bodyToMono(Message.class)
                                .doOnSuccess(message1 -> {})
                                .doOnError((throwable -> {}))
                                .subscribe();
                        continue;
                    }
                }
                //APPROVE 阶段处理逻辑
                if (phase.equals(Phase.APPROVE)) {
                    //批准提案并返回
                    if (CURRENT_ACCEPTED_STATUS.hasChosendValue() && !CURRENT_ACCEPTED_STATUS.getChosenValue().equals(proposal.getContent())) {
                        ACCEPT_PROCESS_LOGGER.warning("!!!!!!!!!!!!!!!!!!!!!!!!the value not consistant!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    }
                    CURRENT_ACCEPTED_STATUS.setChosenValue(proposal.getContent());
                    //返回批准的提案
                    webClient = WebClient.create(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.API_COMMAND_APPROVED_REPLY_CHOSENED_VALUE);
                    proposal.setVoteFrom(local.getIp());
                    webClient.post()
                            .accept(MediaType.APPLICATION_JSON_UTF8)
                            .syncBody(new Message.MessageBuilder<Proposal>()
                                    .setT(proposal)
                                    .setCode(200)
                                    .setMsg("reply chosened value"))
                            .retrieve()
                            .bodyToMono(Message.class).doOnSuccess(message1 -> { })
                            .doOnError((throwable -> {}))
                            .subscribe();

                    //通知其中一个learner学习
                    webClient = WebClient.create(ConstansAndUtils.HTTP_PREFIXX + proposal.getVoteFrom() + ConstansAndUtils.API_COMMAND_APPROVED_LEARNING);
                    proposal.setVoteFrom(local.getIp());
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