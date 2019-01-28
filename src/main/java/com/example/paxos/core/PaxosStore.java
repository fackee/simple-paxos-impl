package com.example.paxos.core;

import com.example.paxos.bean.paxos.*;
import com.example.paxos.exception.SystemNotInitException;
import com.example.paxos.util.ConstansAndUtils;
import com.example.paxos.util.LogUtil;
import org.springframework.util.StringUtils;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaxosStore {

    private static final String NODES_PROPETIES_EQU = "=";

    private static final String NODES_PROPETIES_SPLIT = ";";

    private static final String IP_PORT_SPLIT = ":";

    private static final String PROPETIES_LINE_SPLIT = System.lineSeparator();

    private static final String PROPETIES_NODES_START_WORD = "nodes";

    private static final String PROPETIES_PROPOSOR_START_WORD = "proposor";

    private static final String PROPETIES_ACCEPTOR_START_WORD = "acceptor";

    private static final String PROPETIES_LEARNER_START_WORD = "learner";

    private static final String CLUSTER_PROPERTIES = "./config/server.conf";

    private static final String LEARNING_RECORD = "./logs/learning.log";

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
        FileChannel fileChannel = null;
        File serverPropertiesFile = new File(CLUSTER_PROPERTIES);
        try(FileInputStream fileInputStream = new FileInputStream(serverPropertiesFile)) {
            fileChannel = fileInputStream.getChannel();
            final ByteBuffer byteBuffer = ByteBuffer.allocate((int) serverPropertiesFile.length());
            fileChannel.read(byteBuffer);
            String properties = new String(byteBuffer.array(),"UTF-8");
            if(StringUtils.isEmpty(properties)){
                LogUtil.error("server properties can not be empty");
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
                LogUtil.error("nodes list properties is illegal:" + properties);
                return false;
            }
        } catch (FileNotFoundException e) {
            LogUtil.error(e.getMessage());
            return false;
        } catch (IOException e) {
            LogUtil.error(e.getMessage());
            return false;
        }finally {
            if(fileChannel != null){
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    LogUtil.info(e.getMessage());
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
                if(!nodeStr.contains(IP_PORT_SPLIT)){
                    LogUtil.error("server proper error:{}" + System.lineSeparator() + nodes);
                    System.exit(0);
                }
                CLUSTER.addNode(parse(nodeStr,null));
            }
        }catch (Exception e){
            LogUtil.error("server proper error:{}" + System.lineSeparator() + nodes);
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
                LogUtil.error("server proper error:{}" + nodes + "\n"
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
                        CLUSTER.addNode(parse(nodeArray[nodeIndex++%nodeNumber],new Role(RoleType.PROPOSER)));
                    }
                    continue;
                }
                if(acceptorNumber-- > 0){
                    if(nodeIndex >= nodeNumber){
                        Node assignedNode = CLUSTER.getNodeByIp(nodeArray[nodeIndex++%nodeNumber]);
                        assignedNode.getRole().addRoleType(RoleType.ACCEPTOR);
                    }else{
                        CLUSTER.addNode(parse(nodeArray[nodeIndex++%nodeNumber],new Role(RoleType.ACCEPTOR)));
                    }
                    continue;
                }
                if(learnerNumber-- > 0){
                    if(nodeIndex >= nodeNumber){
                        Node assignedNode = CLUSTER.getNodeByIp(nodeArray[nodeIndex++%nodeNumber]);
                        assignedNode.getRole().addRoleType(RoleType.LEANER);
                    }else{
                        CLUSTER.addNode(parse(nodeArray[nodeIndex++%nodeNumber],new Role(RoleType.LEANER)));
                    }
                    continue;
                }
            }
        }catch (Exception e){
            LogUtil.error("server proper error:{}" + nodes + "\n"
                    + proposor + PROPETIES_LINE_SPLIT
                    + acceptor + PROPETIES_LINE_SPLIT
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

    private static Node parse(String nodeStr,Role role){
        if(!nodeStr.contains(IP_PORT_SPLIT)){
            LogUtil.error("node must like this : ip:port");
            System.exit(0);
        }
        Node node = new Node();
        node.setIp(nodeStr.split(IP_PORT_SPLIT)[0]);
        node.setPort(nodeStr.split(IP_PORT_SPLIT)[1]);
        ConstansAndUtils.PORT = ":" + nodeStr.split(IP_PORT_SPLIT)[1];
        node.setRole(role == null?new Role(true):role);
        return node;
    }

    public static void learning(Proposal proposal) {
        Executors.newFixedThreadPool(5,runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("record-learning-thread");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }).execute(()->{
            persistenceRecord(proposal);
        });
    }

    private static synchronized void persistenceRecord(Proposal proposal){
        File recordLog = new File(LEARNING_RECORD);
        if(!recordLog.exists()){
            try {
                recordLog.createNewFile();
            } catch (IOException e) {
                LogUtil.error("create record record file exception" + e.getMessage());
            }
        }
        recordLog.setWritable(true);
        String appendRecord = new String(proposal.toString() + "\n");
        LogUtil.learning("LEARNING-RECORD:{}", appendRecord);
        try(FileOutputStream outputStream = new FileOutputStream(recordLog,true)) {
            outputStream.write(appendRecord.getBytes("UTF-8"));
            outputStream.flush();
        } catch (FileNotFoundException e) {
            LogUtil.error("record file not exists" + e.getMessage());
        } catch (IOException e) {
            LogUtil.error("write record to log exception" + e.getMessage());
        }
    }
}