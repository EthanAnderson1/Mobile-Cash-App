package com.example.cashappv2;

import java.security.*;
import javax.crypto.Cipher;

public class EncryptDecrypt {

    public static byte[] encrypt(String data, PublicKey key){
        byte[] dataToEncrypt = data.getBytes();
        byte[] encryptedData = null;
        try{
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encryptedData = cipher.doFinal(dataToEncrypt);
        }catch(Exception e){
            e.printStackTrace();
        }
        return encryptedData;
    }
    public String decrypt(byte[] data, Key key){
        String decryptedData = null;
        try{
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            decryptedData = cipher.doFinal(data).toString();

        }catch(Exception e){
            e.printStackTrace();
        }
        return decryptedData;
    }
}
