package com.freightfox.dispatch.exception;

import java.util.List;

public class InvalidRequestException extends RuntimeException {

    private final List<String> errors;

    public InvalidRequestException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
