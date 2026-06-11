package com.abm.leaseFlow.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterTenantRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    private String companyName;

    @NotBlank(message = "Admin first name is required")
    @Size(max = 100)
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    @Size(max = 100)
    private String adminLastName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Must be a valid email address")
    private String adminEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String adminPassword;
}
