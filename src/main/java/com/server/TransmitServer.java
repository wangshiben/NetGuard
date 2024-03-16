package com.server;

import com.configPojo.ServerConfig;
import com.lamba.OnReceivedClientMethod;
import com.lamba.WhileByteRead;

import java.io.*;
import java.net.*;

//public class TestTCPServer {
//    public static void main(String[] args) {
//        TransmitServer transmitServer = new TransmitServer(7788,"localhost",8080);
//        transmitServer.run();
//    }
//}

public class TransmitServer extends Thread {//TCP转发服务,由临时节点转发给内网穿透接受节点
    private final Integer localPort;
    private final ServerConfig serverConfig;
    private final String[] needCheckServerName={"localhost","127.0.0.1"};
    //本地对应端口接收到内网穿透流量转发至下线服务器的操作
    private OnReceivedClientMethod ClientReceiveBytesMethod;
    private WhileByteRead ClientByteRead;
    private OnReceivedClientMethod ServerReceiveBytesMethod;
    private WhileByteRead ServerByteRead;
    private int bufferedSize=1024;//接收到的字节流缓冲区大小，可选操作

    public void setServerReceiveBytesMethod(OnReceivedClientMethod serverReceiveBytesMethod) {
        ServerReceiveBytesMethod = serverReceiveBytesMethod;
    }

    public void setServerByteRead(WhileByteRead serverByteRead) {
        ServerByteRead = serverByteRead;
    }



    public void setBufferedSize(int bufferedSize) {
        if (bufferedSize<10) throw new IllegalArgumentException("缓冲区过小!");
        this.bufferedSize = bufferedSize;
    }

    public void setClientReceiveBytesMethod(OnReceivedClientMethod clientReceiveBytesMethod) {//设置 监听到数据时候的动作:举例:日志打印服务
        this.ClientReceiveBytesMethod = clientReceiveBytesMethod;
    }

    public void setClientByteRead(WhileByteRead clientByteRead) {//接收到数据并要对其修改时候的动作,举例:接收到字节流并加密
        this.ClientByteRead = clientByteRead;
    }

    public TransmitServer(Integer port, String serverName, Integer targetPort) {
        this.localPort = port;
        if (isInvalid(serverName,targetPort)) throw new IllegalArgumentException("监听循环，请检查配置!");
        this.serverConfig=new ServerConfig(serverName,targetPort);
    }
    public TransmitServer(Integer port, ServerConfig serverConfig){
        this(port,serverConfig.getHost(),serverConfig.getPort());
    }
    private boolean isInvalid(String serverName,Integer targetPort){
        for (String name : needCheckServerName) {
            if (name.equals(serverName)&&localPort.equals(targetPort)) return true;
        }
        return false;
    }


    private static void CloseSocket(Socket socket){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        ServerSocket socket = null;
//        ObjectOutputStream outputStream= (ObjectOutputStream) ObjectOutputStream.nullOutputStream();

        try {
            socket = new ServerSocket(localPort);
        } catch (IOException e) {
           if (e instanceof BindException){
               System.out.println("目标端口已被占用,程序即将退出");
               return;
           }

        }
        while (true) {
                if (socket != null) {
                    Socket clientSocket = null;
                    try {
                        clientSocket = socket.accept();
//                        System.out.println("接收到连接:"+ clientSocket.getPort());
                    } catch (IOException e) {
                        continue;
                    }
                    if (clientSocket==null) continue;
                    Socket finalClientSocket = clientSocket;
                    new Thread(()->{
                        // 连接目标服务器
                        Socket targetSocket = null;
                        try {
                            targetSocket = new Socket(serverConfig.getHost(),serverConfig.getPort());
                            System.out.println(Thread.currentThread());
                        } catch (IOException e) {
                           TransmitServer.CloseSocket(finalClientSocket);
                            return;
                        }
                        // 创建线程处理转发
                        ForwardingHandler clientToTargetHandler = null;
                        try {
                            clientToTargetHandler = new ForwardingHandler(finalClientSocket.getInputStream(),this.bufferedSize, targetSocket.getOutputStream(),"readClient:",null);
                            if (this.ClientReceiveBytesMethod !=null) clientToTargetHandler.setOnReceive(this.ClientReceiveBytesMethod);
                            if (this.ClientByteRead !=null) clientToTargetHandler.setWhileByteRead(this.ClientByteRead);
                            Thread clientToTargetThread = new Thread(clientToTargetHandler);
                            clientToTargetThread.start();
//                            System.out.println("已建立与目标端口连接");
                            ForwardingHandler targetToClientHandler = new ForwardingHandler(targetSocket.getInputStream(),this.bufferedSize+100, finalClientSocket.getOutputStream(),"readServer:",null);
                            Thread targetToClientThread = new Thread(targetToClientHandler);
                            if (this.ServerReceiveBytesMethod !=null) clientToTargetHandler.setOnReceive(this.ServerReceiveBytesMethod);
                            if (this.ServerByteRead !=null) clientToTargetHandler.setWhileByteRead(this.ServerByteRead);
                            targetToClientThread.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();

                } else {
                   return;
                }

        }
    }

}

