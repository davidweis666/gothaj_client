package ru.constantdynamic.hpke.crypto.generator;

import ru.constantdynamic.hpke.crypto.param.DerivationParameters;
import ru.constantdynamic.hpke.exceptions.DataLengthException;

public interface DerivationFunction {
    void init(DerivationParameters param);

    int generateBytes(byte[] out, int outOff, int len)
            throws DataLengthException, IllegalArgumentException;
}
