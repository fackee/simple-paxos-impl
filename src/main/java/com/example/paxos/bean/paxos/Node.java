package com.example.paxos.bean.paxos;

import java.util.Objects;

public class Node {

    private String ip;

    private String port;

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

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(getIp(), node.getIp()) &&
                Objects.equals(getPort(), node.getPort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIp(), getPort());
    }

    @Override
    public String toString() {
        return "Node{" +
                "ip='" + ip + '\'' +
                "port=" + port + '\'' +
                ", role=" + role.toString() +
                ", nodeNumber=" + nodeNumber +
                '}';
    }
}