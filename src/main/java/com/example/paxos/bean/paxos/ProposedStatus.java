package com.example.paxos.bean.paxos;

public class ProposedStatus {

    private String lastSendedValue;

    private Integer lastSendedNumber;

    public String getLastSendedValue() {
        return lastSendedValue;
    }

    public void setLastSendedValue(String lastSendedValue) {
        this.lastSendedValue = lastSendedValue;
    }

    public Integer getLastSendedNumber() {
        return lastSendedNumber;
    }

    public void setLastSendedNumber(Integer lastSendedNumber) {
        this.lastSendedNumber = lastSendedNumber;
    }

    @Override
    public String toString() {
        return "ProposedStatus{" +
                "lastSendedValue='" + lastSendedValue + '\'' +
                ", lastSendedNumber=" + lastSendedNumber +
                '}';
    }
}