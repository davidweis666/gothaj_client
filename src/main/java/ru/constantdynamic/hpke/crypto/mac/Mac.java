package ru.constantdynamic.hpke.crypto.mac;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import ru.constantdynamic.hpke.exceptions.DataLengthException;

public interface Mac {
    void init(CipherParameters params) throws IllegalArgumentException;

    int getMacSize();

    void update(byte in) throws IllegalStateException;

    void update(byte[] in, int inOff, int len) throws DataLengthException, IllegalStateException;

    int doFinal(byte[] out, int outOff) throws DataLengthException, IllegalStateException;

    void reset();
}
