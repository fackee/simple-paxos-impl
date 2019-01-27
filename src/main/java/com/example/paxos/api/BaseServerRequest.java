package com.example.paxos.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.io.IOUtils;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;

public class BaseServerRequest {


    protected <T> T parser(String jsonString, TypeReference<T> typeReference){
        if(jsonString == null){
            return null;
        }
        try {
            return JSON.parseObject(jsonString,typeReference);
        } catch (Exception e) {
            return null;
        }
    }

    protected <T> T parser(HttpServletRequest request, TypeReference<T> typeReference){
        try (ServletInputStream inputStream = request.getInputStream(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
            if(inputStream.available() == 0){
                return null;
            }
            IOUtils.copy(inputStream,outputStream);
            return JSON.parseObject(String.valueOf(outputStream.toByteArray()),typeReference);
        } catch (Exception e) {
            return null;
        }
    }

    protected <T> T parser(String jsonString,Class<T> tClass){
        if(jsonString == null){
            return null;
        }
        try {
            return JSON.parseObject(jsonString,tClass);
        } catch (Exception e) {
            return null;
        }
    }


    protected <T> T parser(HttpServletRequest request, Class<T> tClass){
        try (ServletInputStream inputStream = request.getInputStream(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
            if(inputStream.available() == 0){
                return null;
            }
            IOUtils.copy(inputStream,outputStream);
            return JSON.parseObject(String.valueOf(outputStream.toByteArray()),tClass);
        } catch (Exception e) {
            return null;
        }
    }

}