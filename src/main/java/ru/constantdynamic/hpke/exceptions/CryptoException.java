package ru.constantdynamic.hpke.exceptions;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CryptoException extends Exception {
    @Getter
    private Throwable cause;

    public CryptoException(final String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }
}
