package systems.vajzii.irc.util;

import lombok.Getter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class IrcEncrypt {

    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    @Getter
    private boolean active = false;

    public IrcEncrypt() {
        try {
            this.encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            this.decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ciphers", e);
        }
    }

    public void enable(byte[] secretKey) {
        try {
            SecretKey key = new SecretKeySpec(secretKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(secretKey);

            this.encryptCipher.init(Cipher.ENCRYPT_MODE, key, iv);
            this.decryptCipher.init(Cipher.DECRYPT_MODE, key, iv);
            this.active = true;
        } catch (GeneralSecurityException e) {
            e.printStackTrace(System.err);
        }
    }

    public void disable() {
        this.active = false;
    }

    public byte[] encrypt(byte[] input) {
        if (!active) return input;
        try {
            return encryptCipher.update(input);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return input;
        }
    }

    public byte[] decrypt(byte[] input) {
        if (!active) return input;
        try {
            return decryptCipher.update(input);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return input;
        }
    }

    public static class RSAEncrypt {

        public static byte[] encrypt(byte[] data, byte[] publicKeyBytes) {
            try {
                X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey publicKey = kf.generatePublic(spec);

                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                return cipher.doFinal(data);

            } catch (Exception e) {
                e.printStackTrace(System.err);
                return new byte[0];
            }
        }
    }

}
