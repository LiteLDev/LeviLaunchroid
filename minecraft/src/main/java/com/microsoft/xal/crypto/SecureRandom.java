package com.microsoft.xal.crypto;

import androidx.annotation.NonNull;


public class SecureRandom {
    @NonNull
    public static byte[] GenerateRandomBytes(int i) {
        byte[] bArr = new byte[i];
        new java.security.SecureRandom().nextBytes(bArr);
        return bArr;
    }
}