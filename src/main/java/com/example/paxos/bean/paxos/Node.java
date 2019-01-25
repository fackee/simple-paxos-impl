package com.example.paxos.bean.paxos;

public class Node {

    private String ip;

    private Role role;

    private final Integer nodeNumber = Integer.parseInt(String.valueOf(System.nanoTime()).substring(10));

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public final Integer getNodeNumber() {
        return nodeNumber;
    }

    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                ", role=" + role.toString() +
                ", nodeNumber=" + nodeNumber +
                '}';
    }
}