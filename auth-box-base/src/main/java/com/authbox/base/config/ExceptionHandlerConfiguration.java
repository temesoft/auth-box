package com.authbox.base.config;

import com.authbox.base.exception.AccessDeniedException;
import com.authbox.base.exception.BadRequestException;
import com.authbox.base.exception.EntityNotFoundException;
import com.authbox.base.exception.Oauth2Exception;
import com.authbox.base.model.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@ControllerAdvice
public class ExceptionHandlerConfiguration extends ResponseEntityExceptionHandler {

    @Autowired
    private Clock defaultClock;

    @ExceptionHandler(Oauth2Exception.class)
    protected ResponseEntity<Object> handleOauth2Exception(final Oauth2Exception ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                createErrorMap(UNAUTHORIZED.value(), UNAUTHORIZED.getReasonPhrase(), ex.getMessage(), ((ServletWebRequest) request).getRequest().getRequestURI()),
                new HttpHeaders(),
                UNAUTHORIZED,
                request
        );
    }

    @ExceptionHandler(BadRequestException.class)
    protected ResponseEntity<Object> handleBadRequestException(final BadRequestException ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                createErrorMap(BAD_REQUEST.value(), BAD_REQUEST.getReasonPhrase(), ex.getMessage(), ((ServletWebRequest) request).getRequest().getRequestURI()),
                new HttpHeaders(),
                BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFoundException(final EntityNotFoundException ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                createErrorMap(NOT_FOUND.value(), NOT_FOUND.getReasonPhrase(), ex.getMessage(), ((ServletWebRequest) request).getRequest().getRequestURI()),
                new HttpHeaders(),
                NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(final AccessDeniedException ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                createErrorMap(UNAUTHORIZED.value(), UNAUTHORIZED.getReasonPhrase(), ex.getMessage(), ((ServletWebRequest) request).getRequest().getRequestURI()),
                new HttpHeaders(),
                UNAUTHORIZED,
                request
        );
    }

    private ErrorResponse createErrorMap(final int status, final String error, final String message, final String path) {
        return new ErrorResponse(
                Date.from(Instant.now(defaultClock)),
                status,
                error,
                message,
                path
        );
    }
}
