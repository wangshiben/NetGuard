package com.util.encryp;

import com.util.HexUtil;
import com.util.digest.DigestUtil;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

public class TestEncrypt {
    public static void main(String[] args) {
        byte[] bytes = DigestUtil.getSHA256Hash("test".getBytes());
        if (bytes==null) return;
        SM2 sm2=new SM2();
        SM2KeyPair keyPair = sm2.generateKeyPair(new BigInteger(bytes));
        ECPoint publicKey = keyPair.getPublicKey();
        BigInteger x = publicKey.getXCoord().toBigInteger();
        BigInteger y = publicKey.getYCoord().toBigInteger();
        ECPoint pubPoint = SM2.initPubKey(x, y);
        SM2 sm21=new SM2();
        byte[] encrypt = sm21.encrypt("ping", pubPoint);
        byte[] decrypt = sm2.decrypt(encrypt, keyPair.getPrivateKey());
        System.out.println(new String(decrypt));


    }
}
