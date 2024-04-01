package org.ryanair.flight.api.exception;

public class BackendInvocationException extends RuntimeException{
    public BackendInvocationException(String message) {
        super(message);
    }
}
