package ru.constantdynamic.hpke.crypto.cipher;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import ru.constantdynamic.hpke.exceptions.DataLengthException;

public interface StreamCipher {
    void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException;

    byte returnByte(byte in);

    int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException;

    void reset();
}

