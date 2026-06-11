package com.abm.leaseFlow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all domain-specific errors in LeaseFlow.
 */
@Getter
public class LeaseFlowException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public LeaseFlowException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }
}
