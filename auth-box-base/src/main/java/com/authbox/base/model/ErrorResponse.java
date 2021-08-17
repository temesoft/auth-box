package com.authbox.base.model;

import com.google.common.base.MoreObjects;

import java.util.Date;
import java.util.Objects;

public class ErrorResponse {

    public final Date timestamp;
    public final int status;
    public final String error;
    public final String message;
    public final String path;

    public ErrorResponse(final Date timestamp, final int status, final String error, final String message, final String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("timestamp", timestamp)
                .add("status", status)
                .add("error", error)
                .add("message", message)
                .add("path", path)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ErrorResponse that = (ErrorResponse) o;
        return status == that.status &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(error, that.error) &&
                Objects.equals(message, that.message) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, status, error, message, path);
    }
}
