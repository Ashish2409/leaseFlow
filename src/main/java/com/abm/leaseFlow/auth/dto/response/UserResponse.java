package com.abm.leaseFlow.auth.dto.response;

import com.abm.leaseFlow.user.entity.RoleName;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID        userId;
    private UUID        tenantId;
    private String      email;
    private String      firstName;
    private String      lastName;
    private Set<RoleName> roles;
    private boolean     enabled;
    private Instant     createdAt;
}
