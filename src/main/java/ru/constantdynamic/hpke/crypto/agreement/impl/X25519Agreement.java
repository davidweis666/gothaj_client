package ru.constantdynamic.hpke.crypto.agreement.impl;

import ru.constantdynamic.hpke.crypto.agreement.RawAgreement;
import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import ru.constantdynamic.hpke.crypto.param.impl.X25519PrivateKeyParameters;
import ru.constantdynamic.hpke.crypto.param.impl.X25519PublicKeyParameters;

public final class X25519Agreement implements RawAgreement {
    private X25519PrivateKeyParameters privateKey;

    public void init(CipherParameters parameters) {
        this.privateKey = (X25519PrivateKeyParameters) parameters;
    }

    public int getSize() {
        return X25519PrivateKeyParameters.SECRET_SIZE;
    }

    public void calculate(CipherParameters publicKey, byte[] buf, int off) {
        privateKey.generateSecret((X25519PublicKeyParameters) publicKey, buf, off);
    }
}
