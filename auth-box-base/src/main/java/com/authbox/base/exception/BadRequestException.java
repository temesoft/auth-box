package com.authbox.base.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException(final String message) {
        super(message);
    }
}