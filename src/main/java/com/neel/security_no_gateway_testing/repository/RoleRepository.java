package com.neel.security_no_gateway_testing.repository;

import com.neel.security_no_gateway_testing.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Set<Role> findByNameIn(final Set<String> roleNames);

    Optional<Role> findByName(final String roleName);
}
