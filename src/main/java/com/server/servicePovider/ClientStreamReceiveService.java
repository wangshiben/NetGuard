package com.server.servicePovider;

import com.SocketConfig.SecureTransmissionProtocol;
import com.configPojo.ServerConfig;
import com.server.TransmitServer;
import com.util.digest.DigestUtil;
import com.util.encryp.SM2;
import com.util.encryp.SM2KeyPair;

import java.math.BigInteger;

//TODO:从static类中读取配置: 缓冲区
public class ClientStreamReceiveService {//代理主机对应端口接收到TCP连接进行转发 包含功能: 1. 加密数据发送至target主机
    //                                       2. 解密数据发送至外网的socket



    private final TransmitServer server;

    /**
     *
     * @param serverConfig:转发地址配置
     * @param receiveClientPort:本地接收端口
     */
    public ClientStreamReceiveService(ServerConfig serverConfig, int receiveClientPort) {

        this.server = new TransmitServer(receiveClientPort, serverConfig);
        initTransmitServer(this.server);
    }

    public void StartServer() {
        server.start();
    }

    private void initTransmitServer(TransmitServer server) {
//        SM2KeyPair keyPair = initSmKeyPair();
//        if (keyPair != null) {
//            server.setClientByteRead(SecureTransmissionProtocol.decryptData(keyPair));
//            server.setServerByteRead(SecureTransmissionProtocol.encryptData(keyPair.getPublicKey(), targetPort));
//        }
    }

    private SM2KeyPair initSmKeyPair() {
        //TODO:从配置类中读取公私钥信息
        byte[] bytes = DigestUtil.getSHA256Hash("abcd".getBytes());
        if (bytes == null) return null;
        SM2 sm2 = new SM2();
        return sm2.generateKeyPair(new BigInteger(bytes));
    }


}
