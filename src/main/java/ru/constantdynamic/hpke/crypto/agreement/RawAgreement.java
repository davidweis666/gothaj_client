package ru.constantdynamic.hpke.crypto.agreement;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;

public interface RawAgreement {
    void init(final CipherParameters parameters);

    int getSize();

    void calculate(final CipherParameters publicKey, byte[] buf, int off);
}
