package com.abm.leaseFlow.user.repository;

import com.abm.leaseFlow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenantIdAndDeletedAtIsNull(String email, UUID tenantId);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Used during login when tenantId is not provided in the request.
     * Returns the first active user with this email across all tenants.
     * Relies on the idx_users_email index for performance.
     */
    Optional<User> findFirstByEmailAndDeletedAtIsNull(String email);
}
