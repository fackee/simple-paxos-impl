package com.example.paxos.exception;

public class MessageUnsupportException extends RuntimeException{

    public MessageUnsupportException(){
        super();
    }

    public MessageUnsupportException(String message){
        super(message);
    }

    public MessageUnsupportException(String message, Throwable cause){
        super(message,cause);
    }

}