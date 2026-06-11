package com.abm.leaseFlow.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TenantRegistrationResponse {
    private UUID   tenantId;
    private String companyName;
    private UUID   adminUserId;
    private String adminEmail;
}
