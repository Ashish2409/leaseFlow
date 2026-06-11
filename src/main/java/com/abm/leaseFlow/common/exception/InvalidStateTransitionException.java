package com.abm.leaseFlow.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends LeaseFlowException {

    public InvalidStateTransitionException(String entity, Object from, Object to) {
        super("INVALID_STATE_TRANSITION",
              "Cannot transition " + entity + " from " + from + " to " + to,
              HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
