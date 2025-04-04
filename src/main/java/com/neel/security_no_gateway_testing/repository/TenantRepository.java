package com.neel.security_no_gateway_testing.repository;

import com.neel.security_no_gateway_testing.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByGuid(UUID guid);

    Tenant findByName(String tenantName);
}
