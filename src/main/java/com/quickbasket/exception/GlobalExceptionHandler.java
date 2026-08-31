package com.quickbasket.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Global REST exception handler producing RFC 7807 ProblemDetail error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Request Parameters");
        problemDetail.setType(URI.create("https://quickbasket.com/errors/invalid-input"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(ProviderException.class)
    public ProblemDetail handleProviderException(ProviderException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage()
        );
        problemDetail.setTitle("External Provider Unavailable");
        problemDetail.setType(URI.create("https://quickbasket.com/errors/external-provider-unavailable"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
