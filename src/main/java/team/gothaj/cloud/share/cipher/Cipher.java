package team.gothaj.cloud.share.cipher;

public interface Cipher {
    byte[] encrypt(byte[] data);

    byte[] decrypt(byte[] data);
}
