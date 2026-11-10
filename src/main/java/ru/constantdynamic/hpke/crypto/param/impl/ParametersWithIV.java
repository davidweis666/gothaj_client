package ru.constantdynamic.hpke.crypto.param.impl;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import lombok.Getter;

@Getter
public class ParametersWithIV implements CipherParameters {
    private final byte[] iv;
    private final CipherParameters parameters;

    public ParametersWithIV(final CipherParameters parameters, final byte[] iv) {
        this(parameters, iv, 0, iv.length);
    }

    public ParametersWithIV(final CipherParameters parameters, final byte[] iv, int ivOff, int ivLen) {
        this.iv = new byte[ivLen];
        this.parameters = parameters;

        System.arraycopy(iv, ivOff, this.iv, 0, ivLen);
    }
}
