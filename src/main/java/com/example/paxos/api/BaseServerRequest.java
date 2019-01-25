package com.example.paxos.api;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class BaseServerRequest {

    protected <T> T parser(ServerRequest serverRequest,Class<T> tClass){
        if(serverRequest == null){
            return null;
        }
        if(serverRequest.queryParams() == null){
            return null;
        }
        try {
            T bean = tClass.newInstance();
            serverRequest.bodyToMono(tClass).flatMap( message -> {
                BeanUtils.copyProperties(message,bean);
                System.out.println("xxxxxxxxxxxxxxxxxxxxx" + message.toString());
            });
            System.out.println("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzz" + bean.toString());
            return bean;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        Map<String,Object> attr = new HashMap<>(16);
        attr.put("k1","111");
        attr.put("k2","222");
        JSONObject jsonObject = new JSONObject(attr);
        System.out.println(jsonObject.toJSONString());
    }

}