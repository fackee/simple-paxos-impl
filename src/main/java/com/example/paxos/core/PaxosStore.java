package com.example.paxos.core;

import com.example.paxos.bean.paxos.*;
import com.example.paxos.exception.SystemNotInitException;
import io.netty.buffer.ByteBuf;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaxosStore {

    private static final Logger PAXOS_STORE_LOGGER = Logger.getLogger("paxos-store");

    private static final String NODES_PROPETIES_EQU = "=";

    private static final String NODES_PROPETIES_SPLIT = ";";

    private static final String PROPETIES_LINE_SPLIT = "\r\n";

    private static final String PROPETIES_NODES_START_WORD = "nodes";

    private static final String PROPETIES_PROPOSOR_START_WORD = "proposor";

    private static final String PROPETIES_ACCEPTOR_START_WORD = "acceptor";

    private static final String PROPETIES_LEARNER_START_WORD = "learner";

    private static final String CLUSTER_PROPERTIES = "src/main/resources/server.properties";

    private static final Cluster CLUSTER = new Cluster();

    public static boolean load(){
        Future<Boolean> isInit = Executors.newSingleThreadExecutor(runnable -> {
           Thread thread = new Thread(runnable);
           thread.setName("assign cluster thread");
           thread.setPriority(Thread.NORM_PRIORITY);
           return thread;
        }).submit(()->{
            return loadClusterNodes();
        });
        try {
            return isInit.get();
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException e) {
            return false;
        }
    }

    private static synchronized boolean loadClusterNodes(){
        ByteBuffer byteBuffer;
        FileChannel fileChannel = null;
        File serverPropertiesFile = new File(CLUSTER_PROPERTIES);
        try(FileInputStream fileInputStream = new FileInputStream(serverPropertiesFile)) {
            fileChannel = fileInputStream.getChannel();
            byteBuffer = ByteBuffer.allocate((int) serverPropertiesFile.length());
            fileChannel.read(byteBuffer);
            String properties = new String(byteBuffer.array(),"UTF-8");
            if(StringUtils.isEmpty(properties)){
                PAXOS_STORE_LOGGER.log(Level.WARNING,"server properties can not be empyt");
                return false;
            }
            try {
                String[] propotiesLine = properties.split(PROPETIES_LINE_SPLIT);
                int propotiesLineLength = propotiesLine.length;
                if(propotiesLineLength == 1 && propotiesLine[0].startsWith(PROPETIES_NODES_START_WORD)){
                    assignRoleAuto(propotiesLine[0]);
                }else {
                    String nodes = null,proposor=null,acceptor=null,learner=null;
                    for(String prop : propotiesLine){
                        if(prop.startsWith(PROPETIES_NODES_START_WORD)){
                            nodes = prop;
                            continue;
                        }
                        if(prop.startsWith(PROPETIES_PROPOSOR_START_WORD)){
                            proposor = prop;
                            continue;
                        }
                        if(prop.startsWith(PROPETIES_ACCEPTOR_START_WORD)){
                            acceptor = prop;
                            continue;
                        }
                        if(prop.startsWith(PROPETIES_LEARNER_START_WORD)){
                            learner = prop;
                            continue;
                        }
                    }
                    assignRoleCustom(nodes,proposor,acceptor,learner);
                }
            }catch (Exception e){
                PAXOS_STORE_LOGGER.log(Level.WARNING,"nodes list properties is illegal:" + properties);
                return false;
            }
        } catch (FileNotFoundException e) {
            PAXOS_STORE_LOGGER.log(Level.WARNING,e.getMessage());
            return false;
        } catch (IOException e) {
            PAXOS_STORE_LOGGER.log(Level.WARNING,e.getMessage());
            return false;
        }finally {
            if(fileChannel != null){
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    PAXOS_STORE_LOGGER.log(Level.WARNING,e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    private static void assignRoleAuto(String nodes){
        try {
            String[] nodeArray = nodes.substring(nodes.indexOf(PROPETIES_NODES_START_WORD+NODES_PROPETIES_EQU)).split(NODES_PROPETIES_SPLIT);
            for(String nodeStr : nodeArray){
                Node node = new Node();
                node.setIp(nodeStr);
                node.setRole(new Role(true));
                CLUSTER.addNode(node);
            }
        }catch (Exception e){
            PAXOS_STORE_LOGGER.warning("server proper error:" + nodes);
            System.exit(0);
        }
    }

    private static void assignRoleCustom(String nodes,String proposor,String acceptor,String learner){
        try {
            String[] nodeArray = nodes.split((PROPETIES_NODES_START_WORD+NODES_PROPETIES_EQU))[1].split(NODES_PROPETIES_SPLIT);
            int nodeNumber = nodeArray.length;
            int proposorNumber = Integer.parseInt(proposor.split((PROPETIES_PROPOSOR_START_WORD+NODES_PROPETIES_EQU))[1]);
            int acceptorNumber = Integer.parseInt(acceptor.split((PROPETIES_ACCEPTOR_START_WORD+NODES_PROPETIES_EQU))[1]);
            int learnerNumber = Integer.parseInt(learner.split((PROPETIES_LEARNER_START_WORD+NODES_PROPETIES_EQU))[1]);
            boolean propertiesIsLegal = (nodeNumber >= proposorNumber && nodeNumber >= acceptorNumber && nodeNumber >= learnerNumber)
                    || (nodeNumber > proposorNumber + acceptorNumber + learnerNumber);
            if(!propertiesIsLegal){
                PAXOS_STORE_LOGGER.warning("server proper error:" + nodes + "\n"
                        + proposor + "\n"
                        + acceptor + "\n"
                        + learner);
                System.exit(0);
            }
            int nodeIndex = 0;
            while (proposorNumber > 0 || acceptorNumber >0 || learnerNumber >0){
                if(proposorNumber-- > 0){
                    if(nodeIndex >= nodeNumber){
                        Node assignedNode = CLUSTER.getNodeByIp(nodeArray[nodeIndex++%nodeNumber]);
                        assignedNode.getRole().addRoleType(RoleType.PROPOSER);
                    }else{
                        Node prosasorNode = new Node();
                        prosasorNode.setIp(nodeArray[nodeIndex++%nodeNumber]);
                        prosasorNode.setRole(new Role(RoleType.PROPOSER));
                        CLUSTER.addNode(prosasorNode);
                    }
                    continue;
                }
                if(acceptorNumber-- > 0){
                    if(nodeIndex >= nodeNumber){
                        Node assignedNode = CLUSTER.getNodeByIp(nodeArray[nodeIndex++%nodeNumber]);
                        assignedNode.getRole().addRoleType(RoleType.ACCEPTOR);
                    }else{
                        Node acceptorNode = new Node();
                        acceptorNode.setIp(nodeArray[nodeIndex++%nodeNumber]);
                        acceptorNode.setRole(new Role(RoleType.ACCEPTOR));
                        CLUSTER.addNode(acceptorNode);
                    }
                    continue;
                }
                if(learnerNumber-- > 0){
                    if(nodeIndex >= nodeNumber){
                        Node assignedNode = CLUSTER.getNodeByIp(nodeArray[nodeIndex++%nodeNumber]);
                        assignedNode.getRole().addRoleType(RoleType.LEANER);
                    }else{
                        Node leanerNode = new Node();
                        leanerNode.setIp(nodeArray[nodeIndex++%nodeNumber]);
                        leanerNode.setRole(new Role(RoleType.LEANER));
                        CLUSTER.addNode(leanerNode);
                    }
                    continue;
                }
            }
        }catch (Exception e){
            PAXOS_STORE_LOGGER.warning("server proper error:" + nodes + "\n"
                    + proposor + "\n"
                    + acceptor + "\n"
                    + learner);
            System.exit(0);
        }
    }

    public static Cluster getClusterInfo(){
        if(!PaxosCore.isInit){
            throw new SystemNotInitException("paxos system not already initailed");
        }
       return CLUSTER;
    }

    public static void learning(Proposal proposal) {
    }
}