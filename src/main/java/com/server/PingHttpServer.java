package com.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.bouncycastle.math.ec.ECPoint;

public class PingHttpServer {//Http服务器，用于监听端口并返回心跳包 以及公钥
    private final int port;
    private final ECPoint publicKey;
    private int MaxQueue=100;//HTTP最大等待数

    public void setMaxQueue(int maxQueue) {
        if (maxQueue>=0)
        MaxQueue = maxQueue;
    }

    public PingHttpServer(int port, ECPoint publicKey) {
        this.port = port;
        this.publicKey = publicKey;
    }

    public void StartServer(){
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), MaxQueue);

            server.createContext("/", new Handler(this.publicKey));
            server.setExecutor(null); // 使用默认的线程池
            server.start();
            System.out.println("HTTP服务器已启动，监听端口：" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class Handler implements HttpHandler {
    private final ECPoint publicKey;

    public Handler(ECPoint publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "ping"; // 响应内容
        exchange.getResponseHeaders().add("X",publicKey.getXCoord().toBigInteger().toString());
        exchange.getResponseHeaders().add("Y",publicKey.getYCoord().toBigInteger().toString());
        exchange.sendResponseHeaders(200, response.getBytes().length);
        System.out.println("已连接");
//        try {
//            Thread.sleep(1000);
//            System.out.println("模拟大请求完成");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
