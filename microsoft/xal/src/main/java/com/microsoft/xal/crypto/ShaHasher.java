package com.microsoft.xal.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class ShaHasher {
    private final MessageDigest md = MessageDigest.getInstance("SHA-256");

    public ShaHasher() throws NoSuchAlgorithmException {
    }

    public void AddBytes(byte[] bArr) {
        md.update(bArr);
    }

    public byte[] SignHash() {
        return this.md.digest();
    }
}