package com.neel.security_no_gateway_testing.repository;

import com.neel.security_no_gateway_testing.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserIdAndTenantId(UUID userId, Long tenantId);
}
