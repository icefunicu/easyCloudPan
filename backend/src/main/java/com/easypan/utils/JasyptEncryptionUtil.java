package com.easypan.utils;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

public class JasyptEncryptionUtil {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java JasyptEncryptionUtil <secretKey> <valueToEncrypt>");
            System.out.println("Example: java JasyptEncryptionUtil mySecretKey myPassword123");
            return;
        }
        
        String secretKey = args[0];
        String valueToEncrypt = args[1];
        
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPoolSize(1);
        encryptor.setPassword(secretKey);
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        
        String encryptedValue = encryptor.encrypt(valueToEncrypt);
        String decryptedValue = encryptor.decrypt(encryptedValue);
        
        System.out.println("Original value: " + valueToEncrypt);
        System.out.println("Encrypted value: " + encryptedValue);
        System.out.println("Decrypted value: " + decryptedValue);
        System.out.println();
        System.out.println("Use in properties file: ENC(" + encryptedValue + ")");
    }
    
    public static String encrypt(String secretKey, String value) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPoolSize(1);
        encryptor.setPassword(secretKey);
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        return encryptor.encrypt(value);
    }
    
    public static String decrypt(String secretKey, String encryptedValue) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPoolSize(1);
        encryptor.setPassword(secretKey);
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        return encryptor.decrypt(encryptedValue);
    }
}
