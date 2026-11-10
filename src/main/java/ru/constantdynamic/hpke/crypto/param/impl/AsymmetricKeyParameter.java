package ru.constantdynamic.hpke.crypto.param.impl;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;

public class AsymmetricKeyParameter implements CipherParameters {
    boolean privateKey;

    public AsymmetricKeyParameter(boolean privateKey) {
        this.privateKey = privateKey;
    }

    public boolean isPrivateKey() {
        return privateKey;
    }
}
