package com.example.paxos.api;

import com.example.paxos.bean.Phase;
import com.example.paxos.bean.common.Message;
import com.example.paxos.bean.paxos.Proposal;
import com.example.paxos.core.PaxosCore;
import com.example.paxos.exception.UnKnowPhaseException;
import com.example.paxos.util.ConstansAndUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

@RestController
public class PaxosCommand {

    private static final Logger PAXOS_COMMAND_LOGGER = Logger.getLogger("paxos-command-logger");


    @PostMapping(value = ConstansAndUtils.API_COMMAND_PREPARE_SEND_PROPOSAL)
    public Message sendProposal(HttpServletRequest request ,Message message, HttpServletResponse response) throws IOException {
        ServletInputStream inputStream = request.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(inputStream,outputStream);
        System.out.println("======" + outputStream.toByteArray());
        if(Message.isEmpty(message)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
            if(!phase.equals(Phase.PREPARE)){
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
        }catch (Exception e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        PaxosCore.reviceProposalFromProposor(phase,message);
        return new Message.MessageBuilder<>().setCode(200).build();
    }


    @RequestMapping(value = ConstansAndUtils.API_COMMAND_PREPARE_REPLY_PROPOSAL)
    public Message replyProposal(Message message, HttpServletResponse response){
        if(Message.isEmpty(message)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        Object proposal = message.getT();
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
        }catch (UnKnowPhaseException e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if(proposal instanceof Proposal){
            PaxosCore.reviceProposalFromAcceptor(phase,message);
        }
        return new Message.MessageBuilder<>().setCode(200).build();
    }


    @RequestMapping(value = ConstansAndUtils.API_COMMAND_APPROVED_SEND_PROPOSAL)
    public Message approvedSendProposal(Message message, HttpServletResponse response){
        if(Message.isEmpty(message)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
            if(!phase.equals(Phase.APPROVE)){
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
        }catch (UnKnowPhaseException e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        PaxosCore.reviceProposalFromProposor(phase,message);
        return new Message.MessageBuilder<>().setCode(200).build();
    }


    @RequestMapping(value = ConstansAndUtils.API_COMMAND_APPROVED_REPLY_CHOSENED_VALUE)
    public Message replyChosenValue(Message message, HttpServletResponse response){
        if(Message.isEmpty(message)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        PAXOS_COMMAND_LOGGER.info("choosened value:" + message.toString());
        PaxosCore.stopSendProposal(((Proposal)message.getT()).getContent());
        return new Message.MessageBuilder<>().setCode(200).build();
    }


    @RequestMapping(value = ConstansAndUtils.API_COMMAND_APPROVED_LEARNING)
    public Message learning(Message message, HttpServletResponse response){
        if(Message.isEmpty(message)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        Phase phase = null;
        try {
            phase = Phase.getPahse(message.getMessage(),message.getCode());
            if(!phase.equals(Phase.LEARNING)){
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
        }catch (UnKnowPhaseException e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if(!(message.getT() instanceof  Proposal)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        Proposal proposal = (Proposal) message.getT();
        PaxosCore.learning(proposal);
        return new Message.MessageBuilder<>().setCode(200).build();
    }

}