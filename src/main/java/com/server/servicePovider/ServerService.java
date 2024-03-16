package com.server.servicePovider;

import com.SocketConfig.ProtocolHeader;
import com.SocketConfig.SecureTransmissionProtocol;
import com.configPojo.DecryptData;
import com.server.ForwardingHandler;
import com.server.TransmitServer;
import com.util.digest.DigestUtil;
import com.util.encryp.SM2;
import com.util.encryp.SM2KeyPair;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

//TODO:从static类中读取配置: 缓冲区大小 启动端口
public class ServerService {
    private final ServerServiceProvider server;

    public ServerService(int receivePort) {
        SM2KeyPair keyPair = initKeyPair();
        this.server = new ServerServiceProvider(receivePort, keyPair);

    }

    private SM2KeyPair initKeyPair() {
        byte[] bytes = DigestUtil.getSHA256Hash("abcd".getBytes());
        if (bytes == null) return null;
        SM2 sm2 = new SM2();
        return sm2.generateKeyPair(new BigInteger(bytes));
    }
}

class ServerServiceProvider extends Thread {
    private final int localPort;
    private final SM2KeyPair keyPair;
    private final String LocalHost = "127.0.0.1";//TODO:配置类中读取
    private final int bufferedSize = 1024;
    private Selector selector;
   private final   ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final HashMap<SocketChannel, Integer> ThreadHashMap = new HashMap<>();
    private final ReentrantLock reentrantLock = new ReentrantLock();

    ServerServiceProvider(int localPort, SM2KeyPair keyPair) {
        this.localPort = localPort;
        this.keyPair = keyPair;
    }

    @Override
    public void run() {
        ServerSocketChannel socket = null;
//        ObjectOutputStream outputStream= (ObjectOutputStream) ObjectOutputStream.nullOutputStream();
        try {
            socket = ServerSocketChannel.open();
            socket.bind(new InetSocketAddress(localPort));
            socket.configureBlocking(false);
            System.out.println("已开启服务器，端口:" + localPort);
            selector = Selector.open();
            socket.register(selector, SelectionKey.OP_ACCEPT);//交由选择器
        } catch (IOException e) {
            if (e instanceof BindException) {
                System.out.println("目标端口已被占用,程序即将退出");
                return;
            }

        }
        while (true) {
            int select = 0;
            try {
                select = selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (select==0) continue;
            WhileAccept(socket);

        }
    }
    private  void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientSocketChannel = serverSocketChannel.accept();
        clientSocketChannel.configureBlocking(false);


//        SocketAddress remoteAddress = new InetSocketAddress(LocalHost, 8080);
//        SocketChannel targetSocketChannel = SocketChannel.open(remoteAddress);
//        targetSocketChannel.configureBlocking(false);
        // 注册到Selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ);
//        targetSocketChannel.register(selector, SelectionKey.OP_WRITE);
        // 存储关联关系
//        key.attach(new TcpForwardServer.ConnectionPair(clientSocketChannel, targetSocketChannel));
    }

    private  void handleRead(SocketChannel clientSocketChannel) throws IOException {
//        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        if (!clientSocketChannel.isConnected()) return;
        SocketChannel targetSocketChannel = SocketChannel.open(new InetSocketAddress(LocalHost, 8080));
        ByteBuffer buffer = ByteBuffer.allocate(1042);
        int numRead = 0;
        if (clientSocketChannel.isConnected()) {
            while ((numRead = clientSocketChannel.read(buffer)) > 0) {
                buffer.flip();
                ByteBuffer write = ByteBuffer.allocate(1042);
                while (buffer.hasRemaining()) {

                    targetSocketChannel.write(buffer);
                    int read = 0;
                    try {
                        Thread.sleep(10000);//对于没有解析协议的TCP请求，睡眠10MS等待channel中读到所有数据
                        //TODO:可以使用自定义协议来完善
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while ((read = targetSocketChannel.read(write)) > 0) {
                        write.flip();
                        while (write.hasRemaining()) {
                            int writeBytes = clientSocketChannel.write(write);
                            if (writeBytes >= read) {
                                write.clear();
                                break;
                            }
                        }
                        write.flip();
                    }
                    System.out.println("连接准备关闭");
                }


            }

            buffer.clear();
            //else if (numRead == -1) {
//            closeConnection(clientSocketChannel,targetSocketChannel);
//            return;
//        }
            ThreadHashMap.remove(clientSocketChannel);
            closeConnection(clientSocketChannel, targetSocketChannel);
            System.out.println(ThreadHashMap.get(clientSocketChannel));

        }
    }
    private static void closeConnection(SocketChannel... channels) throws IOException {

        for (SocketChannel channel : channels) {
            channel.close();
        }

    }

    //TODO:加上异常终止的情况
    private void WhileAccept(ServerSocketChannel socket){
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            if (key.isAcceptable()) {
                System.out.println("已接收请求");
                try {
                    handleAccept(key);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (key.isReadable()) {
                reentrantLock.lock();
                if (ThreadHashMap.get((SocketChannel) key.channel()) != null) {
                    reentrantLock.unlock();
                    continue;
                }else {
                    ThreadHashMap.put((SocketChannel) key.channel(),SelectionKey.OP_READ);
                    reentrantLock.unlock();
                }

                SocketChannel channel = (SocketChannel) key.channel();
                executorService.submit(() -> {
                    try {
//                                synchronized (channel) {
                        System.out.println("开始读请求");
                        handleRead(channel);
//                                }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

        }

        if (socket != null) {
            SocketChannel clientSocket = null;
            try {
                clientSocket = socket.accept();//接受连接

            } catch (IOException e) {
                return;
            }

            SocketChannel finalClientSocket = clientSocket;
            new Thread(() -> {
                try {
                    ByteBuffer headerBuffer = ByteBuffer.allocate(10);

                    finalClientSocket.read(headerBuffer);
                    byte[] header = headerBuffer.array();//10byte的报文头 主要读取的是转发的端口号
                    System.out.println("接收receiveHeader");

                    //读两次inputStream->第一次，读报文头 第二次读报文体

                    try {


//                                int Magic = dataInputStream.readInt();
//                                short length = dataInputStream.readShort();
//                                int port = dataInputStream.readInt();
//                                byte[] magicByte = SecureTransmissionProtocol.intToByteArray(Magic);
//                                byte[] sho = SecureTransmissionProtocol.shortToByteArray(length);
//                                byte[] portByte = SecureTransmissionProtocol.intToByteArray(port);
//                                header=SecureTransmissionProtocol.concatenateByteArrays(magicByte,sho,portByte);
                        ProtocolHeader protocolHeader = SecureTransmissionProtocol.decryptHeader(header);
                        if (protocolHeader.getPort() != 0) {
                            //表示解析到了请求头
                            ByteBuffer needEncryptData = ByteBuffer.allocate(protocolHeader.getDataLength());
                            int read = 0;
                            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
                            int totalRead=0;
                            while ((read=finalClientSocket.read(needEncryptData))!=protocolHeader.getDataLength()){
                                totalRead+=read;
                            }
                            byteArrayOutputStream.flush();
                            byte[] bytes = needEncryptData.array();
                            SecureTransmissionProtocol.DebugWrite(needEncryptData.array(), "sslbody");
                            SecureTransmissionProtocol.DebugWrite(header, "header");
                            byte[] originData = SecureTransmissionProtocol.concatenateByteArrays(header, bytes);
                            DecryptData decryptData = SecureTransmissionProtocol.decryptData(originData, keyPair.getPrivateKey());
                            Socket targetSocket = new Socket(LocalHost, decryptData.getTargetPort());
                            OutputStream targetSocketOutputStream = targetSocket.getOutputStream();
                            InputStream targetSocketInputStream = targetSocket.getInputStream();
                            if (decryptData.getDecryptData() == null) {
                                System.out.println("debug:"+totalRead+"read: "+read);
                            }
                            System.out.println("成功解析");
                            targetSocketOutputStream.write(decryptData.getDecryptData());//将首个报文体发送至对应端口->开启socket交流
                            targetSocketOutputStream.flush();
                            //开启转发
                            ForwardingHandler clientToTargetHandler = new ForwardingHandler(null, this.bufferedSize + 100, targetSocketOutputStream, "readClient:", null);
                            clientToTargetHandler.setWhileByteRead(SecureTransmissionProtocol.encryptData(keyPair.getPublicKey(), decryptData.getTargetPort()));
                            Thread clientToTargetThread = new Thread(clientToTargetHandler);
                            clientToTargetThread.start();

                            ForwardingHandler targetToClientHandler = new ForwardingHandler(targetSocketInputStream, this.bufferedSize, null, "readServer:", null);
                            targetToClientHandler.setWhileByteRead(SecureTransmissionProtocol.decryptData(keyPair));
                            Thread targetToClientThread = new Thread(targetToClientHandler);

                            targetToClientThread.start();

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();


        } else {
            return;
        }
    }
}
