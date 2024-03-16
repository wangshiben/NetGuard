package com.SocketConfig;

import com.configPojo.DecryptData;
import com.lamba.WhileByteRead;
import com.util.encryp.SM2;
import com.util.encryp.SM2KeyPair;
import org.bouncycastle.math.ec.ECPoint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public class SecureTransmissionProtocol {
    private static final int MagicNumber = 1656890734;
    private static final byte[] magicByte = intToByteArray(MagicNumber);

    //自定义应用层协议:
    //Magic Number 4byte
    //Length 2byte
    //Port 4byte
    // +----------------+----------+-------------+
    //| Magic Number   | Length   | Port        |
    //+----------------+----------+-------------+
    //| Actual Data... |          |             |
    //+----------------+----------+-------------+
    //TODO: 协议头可以抽象为一个对象
    public static byte[] encryptData(byte[] originData, ECPoint publicKey, int forwardPort) {
        SM2 sm2 = new SM2();
        byte[] encryptData = sm2.encrypt(originData, publicKey);
        short dataLength = (short) encryptData.length;//加密后的数据长度
        byte[] shortLength = shortToByteArray(dataLength);
        byte[] portBytes = intToByteArray(forwardPort);
        byte[] resByte = concatenateByteArrays(magicByte, shortLength, portBytes, encryptData);
        return resByte;
    }

    public static DecryptData decryptData(byte[] encryptedData, BigInteger privateKey) {
        ProtocolHeader protocolHeader = decryptHeader(encryptedData);
        if (protocolHeader.getPort() == 0) return new DecryptData(encryptedData, 0);
        byte[] needDecryptedData = new byte[protocolHeader.getDataLength()];
        System.arraycopy(encryptedData, 10, needDecryptedData, 0, protocolHeader.getDataLength());
        SM2 sm2 = new SM2();

        byte[] decrypt = sm2.decrypt(needDecryptedData, privateKey);
        if (decrypt==null){
            System.out.println("解密最后一位为:"+needDecryptedData[needDecryptedData.length-1]+"原数据最后一位为:"+encryptedData[encryptedData.length-1]);
        }
        return new DecryptData(decrypt, protocolHeader.getPort());
    }

    public static ProtocolHeader decryptHeader(byte[] encryptedData) {
        if (encryptedData.length < 10) return new ProtocolHeader(0, 0);
        byte[] MagicNum = new byte[4];
        System.arraycopy(encryptedData, 0, MagicNum, 0, 4);
        if (!Arrays.equals(MagicNum, magicByte)) return new ProtocolHeader(0, 0);
        byte[] dataLength = new byte[2];
        byte[] serverPort = new byte[4];
        System.arraycopy(encryptedData, 4, dataLength, 0, 2);
        System.arraycopy(encryptedData, 6, serverPort, 0, 4);
        short decryptDataLength = byteArrayToShort(dataLength);
        int port = byteArrayToInt(serverPort);
        return new ProtocolHeader(decryptDataLength, port);
    }

    public static WhileByteRead decryptData(SM2KeyPair keyPair) {
        return (bytes, start, end) -> {//负责数据的解密
            byte[] sendBytesSize = new byte[end-start];
            System.arraycopy(bytes, start, sendBytesSize, 0, sendBytesSize.length);
            DecryptData decryptData = SecureTransmissionProtocol.decryptData(sendBytesSize, keyPair.getPrivateKey());
            System.out.printf("已完成数据解密: %s \n",new String(decryptData.getDecryptData()));
            return decryptData.getDecryptData();
        };
    }

    public static WhileByteRead encryptData(ECPoint publicKey,int targetPort) {
        return (bytes, start, end) -> {//负责数据的加密
            byte[] sendBytesSize = new byte[end - start];
            System.arraycopy(bytes, start, sendBytesSize, 0, sendBytesSize.length);
            DebugWrite(sendBytesSize,"first");
            byte[] data = SecureTransmissionProtocol.encryptData(sendBytesSize, publicKey, targetPort);
            DebugWrite(data,"second");
            System.out.println("已完成数据加密,数据长度:"+data.length);
            return data;
        };
    }
    public static void DebugWrite(byte[] data,String name){
        try {
            BufferedWriter writer=new BufferedWriter(new FileWriter("temp_"+name));
            writer.write(new String(data));
            writer.newLine();
            writer.write(Arrays.toString(data).replaceFirst("\\[","{").replaceFirst("]","}"));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static byte[] concatenateByteArrays(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] resultArray = new byte[totalLength];

        int destPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, resultArray, destPos, array.length);
            destPos += array.length;
        }

        return resultArray;
    }

    public static byte[] intToByteArray(int number) {
        byte[] byteArray = new byte[4];

        byteArray[0] = (byte) (number >> 24); // 获取最高位字节
        byteArray[1] = (byte) (number >> 16); // 获取次高位字节
        byteArray[2] = (byte) (number >> 8);  // 获取次低位字节
        byteArray[3] = (byte) number;         // 获取最低位字节

        return byteArray;
    }

    public static int byteArrayToInt(byte[] byteArray) {
        if (byteArray == null || byteArray.length != 4) {
            throw new IllegalArgumentException("Invalid byte array");
        }

        int number = 0;
        number |= (byteArray[0] & 0xFF) << 24; // 设置最高位字节
        number |= (byteArray[1] & 0xFF) << 16; // 设置次高位字节
        number |= (byteArray[2] & 0xFF) << 8;  // 设置次低位字节
        number |= byteArray[3] & 0xFF;         // 设置最低位字节
        return number;
    }

    private static short byteArrayToShort(byte[] byteArray) {
        if (byteArray == null || byteArray.length != 2) {
            throw new IllegalArgumentException("Invalid byte array");
        }

        short number = 0;
        number |= (byteArray[0] & 0xFF) << 8;  // 设置次低位字节
        number |= byteArray[1] & 0xFF;         // 设置最低位字节
        return number;
    }

    public static byte[] shortToByteArray(short number) {
        byte[] byteArray = new byte[2];
        byteArray[0] = (byte) (number >> 8);  // 获取最高位字节
        byteArray[1] = (byte) number;         // 获取最低位字节
        return byteArray;
    }
}
