package com;

import com.server.PingHttpServer;
import com.util.digest.DigestUtil;
import com.util.encryp.SM2;
import com.util.encryp.SM2KeyPair;

import java.math.BigInteger;

public class TestHttpServer {
    public static void main(String[] args) {
        byte[] bytes = DigestUtil.getSHA256Hash("test".getBytes());
        if (bytes==null) return;
        SM2 sm2=new SM2();
        SM2KeyPair keyPair = sm2.generateKeyPair(new BigInteger(bytes));
        PingHttpServer pingHttpServer =new PingHttpServer(8080,keyPair.getPublicKey());
        pingHttpServer.StartServer();
    }
}


