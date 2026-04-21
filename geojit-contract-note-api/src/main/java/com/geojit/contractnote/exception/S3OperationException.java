package com.geojit.contractnote.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class S3OperationException extends RuntimeException {
    public S3OperationException(String message) { super(message); }
    public S3OperationException(String message, Throwable cause) { super(message, cause); }
}
