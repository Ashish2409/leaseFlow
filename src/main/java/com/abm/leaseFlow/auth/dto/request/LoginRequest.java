package com.abm.leaseFlow.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Optional tenant ID. Required when the same email exists in multiple tenants.
     * Can be omitted if the email is globally unique.
     */
    private String tenantId;
}
