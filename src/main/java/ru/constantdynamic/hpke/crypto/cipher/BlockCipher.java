package ru.constantdynamic.hpke.crypto.cipher;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import ru.constantdynamic.hpke.exceptions.DataLengthException;

public interface BlockCipher {
    void init(final boolean forEncryption, final CipherParameters params) throws IllegalArgumentException;

    int getBlockSize();

    int processBlock(final byte[] input, final int inOff, byte[] output, int outOff) throws DataLengthException, IllegalStateException;

    void reset();
}
