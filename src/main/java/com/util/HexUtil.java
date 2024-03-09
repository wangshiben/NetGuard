package com.util;

import java.math.BigInteger;

public class HexUtil {
    public static String encodeToHexStr(byte[] data){
        StringBuilder sb=new StringBuilder();
        sb.append("0x");
        for (byte datum : data) {
            sb.append(encodeByte(datum));
        }
        return sb.toString();
    }
    private static String encodeByte(byte item){
        return String.format("%02x", item);
    }
    public static byte[] decodeToBytes(String hex){
        if (hex.startsWith("0x")) hex=hex.replaceAll("0x","");
        BigInteger integer =new BigInteger(hex,16);
        return integer.toByteArray();
    }



}
