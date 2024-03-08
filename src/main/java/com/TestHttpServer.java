package com;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class TestHttpServer {
    public static void main(String[] args) {
        int port = 8080; // 服务器监听的端口号

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new MyHandler());
            server.setExecutor(null); // 使用默认的线程池
            server.start();
            System.out.println("HTTP服务器已启动，监听端口：" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello, World!"; // 响应内容
            System.out.println(exchange.getRequestHeaders().get("HOST").get(0));
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.flush();
            outputStream.close();
        }
    }
}


