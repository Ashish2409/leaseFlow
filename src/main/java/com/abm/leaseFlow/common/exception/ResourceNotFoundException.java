package com.abm.leaseFlow.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends LeaseFlowException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource.toUpperCase() + "_NOT_FOUND",
              resource + " not found with id: " + id,
              HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
