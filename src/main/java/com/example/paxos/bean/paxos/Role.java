package com.example.paxos.bean.paxos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Role {

    private List<RoleType> roleTypes;

    private String voteFor;

    public Role(){}

    public Role(RoleType roleType){
        roleTypes = new ArrayList<>();
        roleTypes.add(roleType);
    }

    public Role(boolean auto){
        if(auto){
            roleTypes = Arrays.asList(RoleType.PROPOSER,RoleType.ACCEPTOR,RoleType.LEANER);
        }
    }

    public List<RoleType> getRoleTypes() {
        return roleTypes;
    }

    public void setRoleTypes(List<RoleType> roleTypes) {
        this.roleTypes = roleTypes;
    }

    public void addRoleType(RoleType roleType) {
        this.roleTypes.add(roleType);
    }

    public String getVoteFor() {
        return voteFor;
    }

    public void setVoteFor(String voteFor) {
        this.voteFor = voteFor;
    }

    public boolean isProposer(){
        return roleTypes.contains(RoleType.PROPOSER);
    }

    public boolean isAcceptor(){
        return roleTypes.contains(RoleType.ACCEPTOR);
    }

    public boolean isLearner(){
        return roleTypes.contains(RoleType.LEANER);
    }

    @Override
    public String toString() {
        return "Role{" +
                "roleTypes=" + roleTypes.toString() +
                ", voteFor='" + voteFor + '\'' +
                '}';
    }
}