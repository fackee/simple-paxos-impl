package com.example.paxos.bean.common;

public class Message<T> {

    private T t;

    private String message;

    private Integer code;

    public Message(){
        this(null,null);
    }

    public Message(T t) {
        this(t,null);
    }

    public Message(T t, String message) {
        this(t,message,null);
    }

    public Message(T t, String message, Integer code) {
        this.t = t;
        this.message = message;
        this.code = code;
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public static boolean isEmpty(Message that){
        return that == null || that.getMessage() == null || that.getCode() == null || that.getT() == null;
    }


    public static class MessageBuilder<T>{

        private Message message;

        private T t;

        private String msg;

        private Integer code;

        public MessageBuilder(){
            message = new Message(null);
        }

        public MessageBuilder setT(T t) {
            message.setT(t);
            return this;
        }

        public MessageBuilder setMsg(String msg) {
            message.setMessage(msg);
            return this;
        }

        public MessageBuilder setCode(Integer code) {
            message.setCode(code);
            return this;
        }

        public Message build(){
            return this.message;
        }
    }
}