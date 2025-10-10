package com.microsoft.xal.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


public class Ecdsa {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String ECDSA_SIGNATURE_NAME = "NONEwithECDSA";
    private static final String EC_ALGORITHM_NAME = "secp256r1";
    private static final String KEY_ALIAS_PREFIX = "xal_";

    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    private KeyPair keyPair;
    private String uniqueId;

    @Nullable
    public static Ecdsa restoreKeyAndId(@NonNull Context context) throws ClassCastException, IllegalArgumentException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.microsoft.xal.crypto", 0);
        if (!sharedPreferences.contains("id") || !sharedPreferences.contains("public") || !sharedPreferences.contains("private")) {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.clear();
            edit.apply();
            return null;
        }
        String string = sharedPreferences.getString("public", "");
        String string2 = sharedPreferences.getString("private", "");
        String string3 = sharedPreferences.getString("id", "");
        if (string.isEmpty() || string2.isEmpty() || string3.isEmpty()) {
            SharedPreferences.Editor edit2 = sharedPreferences.edit();
            edit2.clear();
            edit2.apply();
            return null;
        }
        byte[] bytesFromBase64String = getBytesFromBase64String(string);
        byte[] bytesFromBase64String2 = getBytesFromBase64String(string2);
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        Ecdsa ecdsa = new Ecdsa();
        ecdsa.uniqueId = string3;
        ecdsa.keyPair = new KeyPair(keyFactory.generatePublic(new X509EncodedKeySpec(bytesFromBase64String)), keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytesFromBase64String2)));
        return ecdsa;
    }

    @NonNull
    @Contract(pure = true)
    private static String getKeyAlias(String str) {
        return KEY_ALIAS_PREFIX + str;
    }

    private static String getBase64StringFromBytes(byte[] bArr) {
        return Base64.encodeToString(bArr, 0, bArr.length, 11);
    }

    private static byte[] getBytesFromBase64String(String str) throws IllegalArgumentException {
        return Base64.decode(str, 11);
    }

    public void generateKey(String str) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "SC");
        keyPairGenerator.initialize(new ECGenParameterSpec(EC_ALGORITHM_NAME));
        this.uniqueId = str;
        this.keyPair = keyPairGenerator.generateKeyPair();
    }

    public EccPubKey getPublicKey() {
        return new EccPubKey((ECPublicKey) this.keyPair.getPublic());
    }

    public String getUniqueId() {
        return this.uniqueId;
    }

    public boolean storeKeyPairAndId(@NonNull Context context, String str) {
        SharedPreferences.Editor edit = context.getSharedPreferences("com.microsoft.xal.crypto", 0).edit();
        edit.putString("id", str);
        edit.putString("public", getBase64StringFromBytes(this.keyPair.getPublic().getEncoded()));
        edit.putString("private", getBase64StringFromBytes(this.keyPair.getPrivate().getEncoded()));
        return edit.commit();
    }

    public byte[] sign(byte[] bArr) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(ECDSA_SIGNATURE_NAME);
        signature.initSign(this.keyPair.getPrivate());
        signature.update(bArr);
        return toP1363SignedBuffer(signature.sign());
    }

    public byte[] hashAndSign(byte[] bArr) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        ShaHasher shaHasher = new ShaHasher();
        shaHasher.AddBytes(bArr);
        return sign(shaHasher.SignHash());
    }

    @NonNull
    private byte[] toP1363SignedBuffer(@NonNull byte[] bArr) {
        byte b = bArr[3];
        int i = 4 + b + 1;
        int i2 = i + 1;
        byte b2 = bArr[i];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        writeAdjustedHalfOfAsn1ToP1363(bArr, 4, b, byteArrayOutputStream);
        writeAdjustedHalfOfAsn1ToP1363(bArr, i2, b2, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void writeAdjustedHalfOfAsn1ToP1363(byte[] bArr, int i, int i2, ByteArrayOutputStream byteArrayOutputStream) {
        if (i2 > 32) {
            byteArrayOutputStream.write(bArr, i + (i2 - 32), 32);
        } else if (i2 < 32) {
            int i3 = 32 - i2;
            byteArrayOutputStream.write(new byte[i3], 0, i3);
            byteArrayOutputStream.write(bArr, i, i2);
        } else {
            byteArrayOutputStream.write(bArr, i, i2);
        }
    }
}