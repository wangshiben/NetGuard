package com;

import com.server.PingServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.util.digest.DigestUtil;
import com.util.encryp.SM2;
import com.util.encryp.SM2KeyPair;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;

public class TestHttpServer {
    public static void main(String[] args) {
        byte[] bytes = DigestUtil.getSHA256Hash("test".getBytes());
        if (bytes==null) return;
        SM2 sm2=new SM2();
        SM2KeyPair keyPair = sm2.generateKeyPair(new BigInteger(bytes));
        PingServer pingServer=new PingServer(8080,keyPair.getPublicKey());
        pingServer.StartServer();
    }
}


