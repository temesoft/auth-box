package com.authbox.base.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(final String message) {
        super(message);
    }
}