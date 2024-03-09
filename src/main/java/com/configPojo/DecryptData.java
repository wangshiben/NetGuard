package com.configPojo;

import java.util.Arrays;

public class DecryptData {
    private final byte[] decryptData;
    private final int targetPort;

    public DecryptData(byte[] decryptData, int targetPort) {
        this.decryptData = decryptData;
        this.targetPort = targetPort;
    }

    public byte[] getDecryptData() {
        return decryptData;
    }

    @Override
    public String toString() {
        return "DecryptData{" +
                "decryptData=" + new String(decryptData) +
                ", targetPort=" + targetPort +
                '}';
    }

    public int getTargetPort() {
        return targetPort;
    }

}
