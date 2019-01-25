package com.example.paxos.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class ConstansAndUtils {

    public static final String HTTP_PREFIXX = "http://";

    public static final String VERSION = "v1";
    public static final String API_COMMAND_PREFFIX = "/" + VERSION + "/paxos";
    public static final String API_COMMAND_PREPARE_SEND_PROPOSAL = API_COMMAND_PREFFIX + "/prepare/sendProposal";
    public static final String API_COMMAND_PREPARE_REPLY_PROPOSAL = API_COMMAND_PREFFIX + "/prepare/replyProposal";

    public static final String API_COMMAND_APPROVED_SEND_PROPOSAL = API_COMMAND_PREFFIX + "/approved/sendProposal";
    //public static final String API_COMMAND_PREPARE_REPLY_PROPOSAL = API_COMMAND_PREFFIX + "/prepare/replyProposal";
    public static final String API_COMMAND_APPROVED_REPLY_CHOSENED_VALUE = API_COMMAND_PREFFIX + "/approved/replyChosenValue";
    public static final String API_COMMAND_APPROVED_LEARNING = API_COMMAND_PREFFIX + "/approved/learning";
    public static final Long PROCESS_SCHEDILE_INIT_DELAY = 1000L;
    public static final Long PROCESS_SCHEDILE_PERIOD = 599L;

    public static final Pattern IP_PATTERN
            = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):?(\\d{1,5})?");

    private static String quorum;

    static {
        //get the quorum from properties,if null,set the majority (num(acceptors)/2) of clusters as quorum
        // 获取配置中的quorum，如果没有则取集群中acceptor多数派即 num(acceptors)/2 + 1
    }


    public static final boolean isIpString(String ip){
        return IP_PATTERN.matcher(ip).matches();
    }

    public static String getHostIP(){

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            hostIp = null;
        }
        return hostIp;
    }

    public static void main(String[] args) {
        String ip = getHostIP();
        System.out.println(ip);
        System.out.println(isIpString(ip));
    }

}