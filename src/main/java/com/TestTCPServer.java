package com;

import com.configPojo.ServerConfig;
import com.lamba.OnReceivedClientMethod;
import com.lamba.WhileByteRead;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestTCPServer {
    public static void main(String[] args) {
        Servers servers = new Servers(7788,"localhost",8080);
        servers.run();
    }
}

class Servers extends Thread {//TCP转发服务,由临时节点转发给接受节点
    private final Integer localPort;
    private final ServerConfig serverConfig;
    private final String[] needCheckServerName={"localhost","127.0.0.1"};
    public Servers(Integer port,String serverName,Integer targetPort) {
        this.localPort = port;
        if (isInvalid(serverName,targetPort)) throw new IllegalArgumentException("监听循环，请检查配置!");
        this.serverConfig=new ServerConfig(serverName,targetPort);
    }
    public Servers(Integer port,ServerConfig serverConfig){
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

                        clientToTargetHandler = new ForwardingHandler(clientSocket.getInputStream(), targetSocket.getOutputStream(),"readClient:",null);
                        clientToTargetHandler.setOnReceive( bytes->{
                            System.out.print(new String(bytes));
                        });
                        Thread clientToTargetThread = new Thread(clientToTargetHandler);
                        clientToTargetThread.start();

                        ForwardingHandler targetToClientHandler = new ForwardingHandler(targetSocket.getInputStream(), clientSocket.getOutputStream(),"readServer:",null);
                        Thread targetToClientThread = new Thread(targetToClientHandler);
                        targetToClientThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.exit(0);
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

    public ForwardingHandler(InputStream inputStream, OutputStream outputStream,String name,OnReceivedClientMethod receivedClientMethod) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.name=name;
        this.receivedClientMethod=receivedClientMethod;
    }

    public void setOnReceive(OnReceivedClientMethod onReceive){
        this.receivedClientMethod=onReceive;
    }

    public void setWhileByteRead(WhileByteRead whileByteRead) {
        this.whileByteRead = whileByteRead;
    }

    public ForwardingHandler(InputStream inputStream, OutputStream outputStream, OnReceivedClientMethod receivedClientMethod) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.receivedClientMethod = receivedClientMethod;
        this.name=Thread.currentThread().getName();
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
