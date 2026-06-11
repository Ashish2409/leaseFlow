package com.abm.leaseFlow.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends LeaseFlowException {

    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message, HttpStatus.CONFLICT);
    }
}
