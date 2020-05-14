package com.example.cashappv2;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.*;
import java.util.Base64;

import javax.crypto.Cipher;

public class SecurityHelper {

    public static byte[] encrypt(String data, PublicKey key) {
        byte[] dataToEncrypt = data.getBytes();
        byte[] encryptedData = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encryptedData = cipher.doFinal(dataToEncrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedData;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(byte[] data, PrivateKey key) {
        String decryptedData = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            Base64.Decoder decoder = Base64.getDecoder();
            cipher.init(Cipher.DECRYPT_MODE, key);
            decryptedData = new String(cipher.doFinal(decoder.decode(data)));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedData;
    }
}