package com.authbox.base.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ErrorResponse {

    public final Date timestamp;
    public final int status;
    public final String error;
    public final String message;
    public final String path;
}
