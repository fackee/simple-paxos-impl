package com.example.paxos.bean.paxos;

public class Proposal {

    private String content;

    private Integer number;

    private String voteFrom;

    private Boolean hasChoosen;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getVoteFrom() {
        return voteFrom;
    }

    public void setVoteFrom(String voteFrom) {
        this.voteFrom = voteFrom;
    }

    public Boolean isHasChoosen() {
        return hasChoosen;
    }

    public void setHasChoosen(Boolean hasChoosen) {
        this.hasChoosen = hasChoosen;
    }

    @Override
    public String toString() {
        return "Proposal{" +
                "content='" + content + '\'' +
                ", number=" + number +
                ", voteFrom='" + voteFrom + '\'' +
                ", hasChoosen=" + hasChoosen +
                '}';
    }
}