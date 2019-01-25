package com.example.paxos.api.router;

import com.example.paxos.api.PaxosCommand;
import com.example.paxos.bean.common.Message;
import com.example.paxos.core.PaxosCore;
import com.example.paxos.util.ConstansAndUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Component
public class RouterConfig {

    @Autowired
    private PaxosCommand paxosCommand;

    @Bean
    public RouterFunction<ServerResponse> paxosCommands(){
        return RouterFunctions
                .route(POST(ConstansAndUtils.API_COMMAND_PREPARE_SEND_PROPOSAL),paxosCommand::sendProposal)
                .andRoute(POST(ConstansAndUtils.API_COMMAND_PREPARE_REPLY_PROPOSAL),paxosCommand::replyProposal)
                .andRoute(POST(ConstansAndUtils.API_COMMAND_APPROVED_SEND_PROPOSAL),paxosCommand::approvedSendProposal)
                .andRoute(POST(ConstansAndUtils.API_COMMAND_APPROVED_REPLY_CHOSENED_VALUE),paxosCommand::replyChosenValue)
                .andRoute(POST(ConstansAndUtils.API_COMMAND_APPROVED_LEARNING),paxosCommand::learning);
    }

}