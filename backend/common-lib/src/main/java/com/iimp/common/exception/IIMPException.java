package com.iimp.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IIMPException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public IIMPException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static IIMPException notFound(String resource, String id) {
        return new IIMPException(
            resource + " not found: " + id,
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }

    public static IIMPException badRequest(String message) {
        return new IIMPException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static IIMPException unauthorized(String message) {
        return new IIMPException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static IIMPException forbidden(String message) {
        return new IIMPException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static IIMPException conflict(String message) {
        return new IIMPException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static IIMPException internalError(String message) {
        return new IIMPException(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }
}
