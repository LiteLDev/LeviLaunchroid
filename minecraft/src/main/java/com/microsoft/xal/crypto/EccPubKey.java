package com.microsoft.xal.crypto;

import android.util.Base64;
import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;


public class EccPubKey {
    private final ECPublicKey publicKey;

    public EccPubKey(ECPublicKey eCPublicKey) {
        this.publicKey = eCPublicKey;
    }

    public BigInteger getX() {
        return this.publicKey.getW().getAffineX();
    }

    public String getBase64UrlX() {
        return getBase64Coordinate(getX());
    }

    public BigInteger getY() {
        return this.publicKey.getW().getAffineY();
    }

    public String getBase64UrlY() {
        return getBase64Coordinate(getY());
    }

    private String getBase64Coordinate(@NonNull BigInteger bigInteger) {
        byte[] byteArray = bigInteger.toByteArray();
        int i = 0;
        if (byteArray.length > 32) {
            i = byteArray.length - 32;
        } else if (byteArray.length < 32) {
            byte[] bArr = new byte[32];
            System.arraycopy(byteArray, 0, bArr, 32 - byteArray.length, byteArray.length);
            byteArray = bArr;
        }
        return Base64.encodeToString(byteArray, i, 32, 11);
    }
}