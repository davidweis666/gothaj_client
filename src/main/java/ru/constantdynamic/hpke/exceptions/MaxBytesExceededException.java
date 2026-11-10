package ru.constantdynamic.hpke.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MaxBytesExceededException extends RuntimeCryptoException {
    public MaxBytesExceededException(final String message) {
        super(message);
    }
}
