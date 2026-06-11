package com.abm.leaseFlow.tenant.repository;

import com.abm.leaseFlow.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameIgnoreCase(String name);
}
