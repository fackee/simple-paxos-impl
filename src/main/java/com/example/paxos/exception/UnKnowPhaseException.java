package com.example.paxos.exception;

public class UnKnowPhaseException extends RuntimeException{

    public UnKnowPhaseException(){
        super();
    }

    public UnKnowPhaseException(String message){
        super(message);
    }

    public UnKnowPhaseException(String message, Throwable cause){
        super(message,cause);
    }
}