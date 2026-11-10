package ru.constantdynamic.hpke.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class InvalidCipherTextException extends CryptoException {
    public InvalidCipherTextException(final String message) {
        super(message);
    }

    public InvalidCipherTextException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
