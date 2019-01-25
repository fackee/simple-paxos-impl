package com.example.paxos.exception;

public class OperatorUnSupportException extends RuntimeException{


    public OperatorUnSupportException(){
        super();
    }

    public OperatorUnSupportException(String message){
        super(message);
    }

    public OperatorUnSupportException(String message, Throwable cause){
        super(message,cause);
    }
}