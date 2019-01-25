package com.example.paxos.bean.paxos;

public class AcceptedStatus {

    private String lastAcceptedValue;

    private Integer lastAcceptedNumber;

    private String chosenValue;

    public String getLastAcceptedValue() {
        return lastAcceptedValue;
    }

    public void setLastAcceptedValue(String lastAcceptedValue) {
        this.lastAcceptedValue = lastAcceptedValue;
    }

    public Integer getLastAcceptedNumber() {
        return lastAcceptedNumber;
    }

    public void setLastAcceptedNumber(Integer lastAcceptedNumber) {
        this.lastAcceptedNumber = lastAcceptedNumber;
    }

    public String getChosenValue() {
        return chosenValue;
    }

    public void setChosenValue(String chosenValue) {
        this.chosenValue = chosenValue;
    }

    public boolean isFirstAcceptProposal(){
        return this.chosenValue == null && this.lastAcceptedNumber == null
                && this.lastAcceptedValue == null;
    }

    public boolean hasChosendValue(){
        return this.chosenValue != null;
    }

    public boolean greaterThanLastProposalNumber(Integer inputProposalNumber){
        return inputProposalNumber > this.lastAcceptedNumber;
    }



    @Override
    public String toString() {
        return "AcceptedStatus{" +
                "lastAcceptedValue='" + lastAcceptedValue + '\'' +
                ", lastAcceptedNumber=" + lastAcceptedNumber +
                ", chosenValue='" + chosenValue + '\'' +
                '}';
    }
}