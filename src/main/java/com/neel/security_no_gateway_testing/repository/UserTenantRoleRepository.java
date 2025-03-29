package com.neel.security_no_gateway_testing.repository;

import com.neel.security_no_gateway_testing.entity.UserTenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserTenantRoleRepository extends JpaRepository<UserTenantRole, UUID> {
}
