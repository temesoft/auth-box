package com.authbox.base.exception;

import static com.authbox.base.config.Constants.MSG_ACCESS_DENIED;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super(MSG_ACCESS_DENIED);
    }
}