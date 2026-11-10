package ru.constantdynamic.hpke.crypto.param.impl;

import ru.constantdynamic.hpke.crypto.param.CipherParameters;
import ru.constantdynamic.hpke.misc.Arrays;
import lombok.Getter;

@Getter
public class KeyParameter implements CipherParameters {
    private final byte[] key;

    public KeyParameter(byte[] key) {
        this(key, 0, key.length);
    }

    public KeyParameter(byte[] key, int keyOff, int keyLen) {
        this(keyLen);

        System.arraycopy(key, keyOff, this.key, 0, keyLen);
    }

    private KeyParameter(int length) {
        this.key = new byte[length];
    }

    public void copyTo(byte[] buf, int off, int len) {
        if (key.length != len) {
            throw new IllegalArgumentException("Key length doesn't equal to the given length.");
        }

        System.arraycopy(key, 0, buf, off, len);
    }

    public int getKeyLength() {
        return key.length;
    }

    public KeyParameter reverse() {
        final KeyParameter reversed = new KeyParameter(key.length);
        Arrays.reverse(this.key, reversed.key);

        return reversed;
    }
}
