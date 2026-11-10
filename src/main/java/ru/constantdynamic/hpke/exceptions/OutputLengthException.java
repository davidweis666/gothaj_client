package ru.constantdynamic.hpke.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class OutputLengthException extends DataLengthException {
    public OutputLengthException(final String message) {
        super(message);
    }
}
