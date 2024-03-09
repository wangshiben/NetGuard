package com.server;

import com.configPojo.ServerConfig;
import com.lamba.OnReceivedClientMethod;
import com.lamba.WhileByteRead;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

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
        if (bufferedSize==0) throw new IllegalArgumentException("缓冲区大小不能为0!");
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


    private void CloseSocket(Socket socket){
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
                    } catch (IOException e) {
                        continue;
                    }

                    // 连接目标服务器
                    Socket targetSocket = null;
                    try {
                        targetSocket = new Socket(serverConfig.getHost(),serverConfig.getPort());
                    } catch (IOException e) {
                       CloseSocket(clientSocket);
                       continue;
                    }
                    // 创建线程处理转发
                    ForwardingHandler clientToTargetHandler = null;
                    try {

                        clientToTargetHandler = new ForwardingHandler(clientSocket.getInputStream(),this.bufferedSize, targetSocket.getOutputStream(),"readClient:",null);
                       if (this.ClientReceiveBytesMethod !=null) clientToTargetHandler.setOnReceive(this.ClientReceiveBytesMethod);
                       if (this.ClientByteRead !=null) clientToTargetHandler.setWhileByteRead(this.ClientByteRead);
                        Thread clientToTargetThread = new Thread(clientToTargetHandler);
                        clientToTargetThread.start();

                        ForwardingHandler targetToClientHandler = new ForwardingHandler(targetSocket.getInputStream(),this.bufferedSize, clientSocket.getOutputStream(),"readServer:",null);
                        Thread targetToClientThread = new Thread(targetToClientHandler);
                        if (this.ServerReceiveBytesMethod !=null) clientToTargetHandler.setOnReceive(this.ServerReceiveBytesMethod);
                        if (this.ServerByteRead !=null) clientToTargetHandler.setWhileByteRead(this.ServerByteRead);
                        targetToClientThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                   return;
                }

        }
    }

    private ByteArrayOutputStream readAllBytes(InputStream stream) throws IOException {
        byte[] buffered = new byte[1024];
        int off = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while ((off = stream.read(buffered)) != -1) {

            outputStream.write(buffered, 0, off);
        }
        return outputStream;
    }
}

class ForwardingHandler implements Runnable {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final String name;
    private OnReceivedClientMethod receivedClientMethod;
    private final ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
    private WhileByteRead whileByteRead;
    private final int bufferedSize;

    public ForwardingHandler(InputStream inputStream,int bufferedSize, OutputStream outputStream,String name,OnReceivedClientMethod receivedClientMethod) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.name=name;
        this.receivedClientMethod=receivedClientMethod;
        this.bufferedSize=bufferedSize;
    }

    public void setOnReceive(OnReceivedClientMethod onReceive){
        this.receivedClientMethod=onReceive;
    }

    public void setWhileByteRead(WhileByteRead whileByteRead) {
        this.whileByteRead = whileByteRead;
    }

    public ForwardingHandler(InputStream inputStream,int bufferedSize, OutputStream outputStream, OnReceivedClientMethod receivedClientMethod) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.receivedClientMethod = receivedClientMethod;
        this.name=Thread.currentThread().getName();
        this.bufferedSize=bufferedSize;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[24];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (this.whileByteRead!=null) buffer = this.whileByteRead.ReadByte(buffer,0,bytesRead);
                outputStream.write(buffer, 0, bytesRead);
                byteArrayOutputStream.write(buffer,0,bytesRead);
                if (this.receivedClientMethod!=null) this.receivedClientMethod.OnReceiveClientBytes(byteArrayOutputStream.toByteArray());
                byteArrayOutputStream.flush();
                outputStream.flush();
            }
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
