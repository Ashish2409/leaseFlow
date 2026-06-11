package com.abm.leaseFlow.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends LeaseFlowException {

    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", message, HttpStatus.UNAUTHORIZED);
    }
}
