package com.quickbasket.exception;

/**
 * Custom runtime exception thrown when a third-party product provider invocation fails.
 */
public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
