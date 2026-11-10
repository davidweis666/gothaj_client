package ru.constantdynamic.hpke.crypto.cipher;

public interface SkippingCipher {
    long skip(long numberOfBytes);

    long seekTo(long position);

    long getPosition();
}

