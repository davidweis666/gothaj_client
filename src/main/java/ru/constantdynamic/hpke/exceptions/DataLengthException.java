package ru.constantdynamic.hpke.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DataLengthException extends RuntimeCryptoException {
    public DataLengthException(final String message) {
        super(message);
    }
}
