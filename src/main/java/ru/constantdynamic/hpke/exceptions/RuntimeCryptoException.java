package ru.constantdynamic.hpke.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RuntimeCryptoException extends RuntimeException {
    public RuntimeCryptoException(final String message) {
        super(message);
    }
}
