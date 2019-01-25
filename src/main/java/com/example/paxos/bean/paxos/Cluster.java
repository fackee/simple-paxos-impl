package com.example.paxos.bean.paxos;

import com.example.paxos.util.ConstansAndUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Cluster {

    private final Map<String,Node> NODE_MAP = new ConcurrentHashMap<>(16);

    public Cluster(){}

    public Cluster(Node node){
        this(new ArrayList<Node>(){{add(node);}});
    }

    public Cluster(List<Node> nodes){
        nodes.forEach( node -> {
            if(node.getIp() != null &&  ConstansAndUtils.isIpString(node.getIp())){
                NODE_MAP.put(node.getIp(),node);
            }else {
                throw new IllegalArgumentException("node is illegal:" + node.toString());
            }
        });
    }

    public void addNode(Node node){
        if(node.getIp() != null && ConstansAndUtils.isIpString(node.getIp())){
            NODE_MAP.put(node.getIp(),node);
            return;
        }
        throw new IllegalArgumentException("node ip can not be null");
    }

    public Node getNodeByIp(String ip){
        return NODE_MAP.get(ip);
    }

    public Set<Node> getAllNode(){
        final Set<Node> nodeSet = new HashSet<>();
        NODE_MAP.values().forEach(node -> {
            nodeSet.add(node);
        });
        return nodeSet;
    }
}