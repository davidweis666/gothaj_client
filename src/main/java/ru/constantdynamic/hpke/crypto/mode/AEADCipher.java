package ru.constantdynamic.hpke.crypto.mode;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import ru.constantdynamic.hpke.exceptions.DataLengthException;
import ru.constantdynamic.hpke.exceptions.InvalidCipherTextException;

public interface AEADCipher {
    void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException;

    void processAADByte(byte in);

    void processAADByte(byte[] in, int inOff, int len);

    int processByte(byte in, byte[] out, int outOff) throws DataLengthException;

    int processByte(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException;

    int doFinal(byte[] out, int outOff) throws DataLengthException, InvalidCipherTextException;

    byte[] getMac();

    int getUpdateOutputSize(int len);

    int getOutputSize(int len);

    void reset();
}
