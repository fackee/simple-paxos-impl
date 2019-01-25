package com.example.paxos.exception;

public class SystemNotInitException extends RuntimeException{

    public SystemNotInitException(){
        super();
    }

    public SystemNotInitException(String message){
        super(message);
    }

    public SystemNotInitException(String message, Throwable cause){
        super(message,cause);
    }
}