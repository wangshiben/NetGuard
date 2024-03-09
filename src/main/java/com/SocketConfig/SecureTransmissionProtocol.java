package com.SocketConfig;

import com.configPojo.DecryptData;
import com.util.encryp.SM2;
import com.util.encryp.SM2KeyPair;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.Arrays;

public class SecureTransmissionProtocol {
    private static final int MagicNumber=1656890734;
    private static final byte[] magicByte=intToByteArray(MagicNumber);

    //自定义应用层协议:
    // +----------------+----------+-------------+
    //| Magic Number   | Length   | Port        |
    //+----------------+----------+-------------+
    //| Actual Data... |          |             |
    //+----------------+----------+-------------+
    public static byte[] encryptData(byte[] originData, ECPoint publicKey,int forwardPort){
        SM2 sm2=new SM2();
        byte[] encryptData = sm2.encrypt(originData, publicKey);
        short dataLength= (short) encryptData.length;
        byte[] shortLength = shortToByteArray(dataLength);
        byte[] portBytes = shortToByteArray((short) forwardPort);
        byte[] resByte=concatenateByteArrays(magicByte,shortLength,portBytes,encryptData);
        return resByte;
    }

    public static DecryptData decryptData(byte[]encryptedData, BigInteger privateKey){
        if (encryptedData.length<8) return null;
        byte[] MagicNum=new byte[4];
        if (!Arrays.equals(MagicNum, magicByte)) return null;
        byte[] dataLength=new byte[2];
        byte[] serverPort=new byte[2];
        System.arraycopy(encryptedData,0,MagicNum,0,4);
        System.arraycopy(encryptedData,4,dataLength,0,2);
        System.arraycopy(encryptedData,6,serverPort,0,2);
        short decryptDataLength = byteArrayToShort(dataLength);

        byte[]needDecryptedData=new byte[decryptDataLength];
        System.arraycopy(encryptedData,8,needDecryptedData,0,decryptDataLength);
        SM2 sm2=new SM2();
        SM2KeyPair keyPair = sm2.generateKeyPair(privateKey);
        short port = byteArrayToShort(serverPort);
        byte[] decrypt = sm2.decrypt(needDecryptedData, privateKey);
        return new DecryptData(decrypt,port);
    }


    private static byte[] concatenateByteArrays(byte[]... arrays) {
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

    private static byte[] intToByteArray(int number) {
        byte[] byteArray = new byte[4];

        byteArray[0] = (byte) (number >> 24); // 获取最高位字节
        byteArray[1] = (byte) (number >> 16); // 获取次高位字节
        byteArray[2] = (byte) (number >> 8);  // 获取次低位字节
        byteArray[3] = (byte) number;         // 获取最低位字节

        return byteArray;
    }
    private static int byteArrayToInt(byte[] byteArray) {
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
    private static byte[] shortToByteArray(short number) {
        byte[] byteArray = new byte[2];
        byteArray[0] = (byte) (number >> 8);  // 获取最高位字节
        byteArray[1] = (byte) number;         // 获取最低位字节
        return byteArray;
    }
}
