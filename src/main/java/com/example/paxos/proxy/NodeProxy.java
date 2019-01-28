package com.example.paxos.proxy;

import com.example.paxos.bean.paxos.Cluster;
import com.example.paxos.bean.paxos.Node;
import com.example.paxos.bean.paxos.RoleType;
import com.example.paxos.core.PaxosStore;
import com.example.paxos.util.ConstansAndUtils;
import sun.dc.pr.PRError;

import java.awt.image.VolatileImage;
import java.util.HashSet;
import java.util.Set;

public class NodeProxy {

    private static volatile String choosenedValue = null;

    public Set<Node> getAllAcceptors() {
        return getNodesByType(RoleType.ACCEPTOR);
    }

    public Set<Node> getAllProposor() {
        return getNodesByType(RoleType.PROPOSER);
    }

    public Set<Node> getAllLearner() {
        return getNodesByType(RoleType.LEANER);
    }

    private Set<Node> getNodesByType(RoleType roleType) {
        Set<Node> allNode = new HashSet<>(PaxosStore.getClusterInfo().getAllNode());
        allNode.forEach(node -> {
            if (roleType.equals(RoleType.ACCEPTOR) && !node.getRole().isAcceptor()) {
                allNode.remove(node);
            }
            if (roleType.equals(RoleType.PROPOSER) && !node.getRole().isProposer()) {
                allNode.remove(node);
            }
            if (roleType.equals(RoleType.LEANER) && !node.getRole().isLearner()) {
                allNode.remove(node);
            }
        });
        return allNode;
    }

    public Node getNodeByIp(String ip){
        return PaxosStore.getClusterInfo().getNodeByIp(ip);
    }

    public Set<Node> getMojorityAcceptors() {
        Set<Node> nodeSet = new HashSet<>(16);
        Node[] allNodes = getAllAcceptors().toArray(new Node[]{});
        for (int i = 0; i < allNodes.length / 2 + 1; i++) {
            nodeSet.add(allNodes[i]);
        }
        return nodeSet;
    }

    public boolean isProposor(String ip) {
        final boolean[] isProposor = {false};
        Set<Node> nodeSet = new HashSet<>(getAllProposor());
        nodeSet.forEach(node -> {
            if (node.getIp().equals(ip)) {
                isProposor[0] = true;
                return;
            }
        });
        return isProposor[0];
    }

    public Node getLocalServer() {
        return PaxosStore.getClusterInfo().getNodeByIp(ConstansAndUtils.getHostIP());
    }

    public String choosenedValue() {
        return choosenedValue;
    }

    public void choosenedValue(String choosenedValue) {
        NodeProxy.choosenedValue = choosenedValue;
    }

    public enum NodeProxyInstance {

        INSTANCE;

        private NodeProxy nodeProxy = null;

        private NodeProxyInstance() {
            nodeProxy = new NodeProxy();
        }

        public NodeProxy getInstance() {
            return nodeProxy;
        }

    }


}