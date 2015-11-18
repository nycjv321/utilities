package com.nycjv321.utilities.http.exceptions;

/**
 * This unchecked exception represents all http-related exceptions encountered in this class.
 */
public final class HttpException extends RuntimeException {
    public HttpException(String message) {
        super(message);
    }
}
