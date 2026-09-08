package com.adtech.iaterap.backend.validation;

public class ValidationFixtureException extends IllegalArgumentException {

    public ValidationFixtureException(String message) {
        super(message);
    }

    public ValidationFixtureException(String message, Throwable cause) {
        super(message, cause);
    }
}
