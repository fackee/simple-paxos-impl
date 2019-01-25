package com.example.paxos.bean;

import com.example.paxos.exception.UnKnowPhaseException;

public enum  Phase {

    PREPARE("PREPARE",1000),
    APPROVE("APPROVE",10001),
    LEARNING("LEARNING",10002);
    private String phase;

    private Integer code;

    private Phase(String phase,Integer code){
        this.phase = phase;
        this.code = code;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public static Phase getPahse(String phase,Integer code){
        if(phase.equals(Phase.PREPARE.getPhase()) && code.equals(Phase.PREPARE.code)){
            return Phase.PREPARE;
        }
        if(phase.equals(Phase.APPROVE.getPhase()) && code.equals(Phase.APPROVE.code)){
            return Phase.APPROVE;
        }
        if(phase.equals(Phase.LEARNING.getPhase()) && code.equals(Phase.LEARNING.code)){
            return Phase.LEARNING;
        }
        throw new UnKnowPhaseException("unknow phase:[" + phase + "," + code + "]");
    }
}