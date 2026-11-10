package ru.constantdynamic.hpke.crypto.digest;

public interface Digest {
    String getAlgorithmName();

    int getDigestSize();

    void update(byte in);

    void update(byte[] in, int inOff, int len);

    int doFinal(byte[] out, int outOff);

    void reset();
}
