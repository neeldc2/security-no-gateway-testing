package com.neel.security_no_gateway_testing.service;

import com.neel.security_no_gateway_testing.entity.Role;
import com.neel.security_no_gateway_testing.entity.Tenant;
import com.neel.security_no_gateway_testing.entity.User;
import com.neel.security_no_gateway_testing.entity.UserTenantRole;
import com.neel.security_no_gateway_testing.exception.WebsiteException;
import com.neel.security_no_gateway_testing.repository.RoleRepository;
import com.neel.security_no_gateway_testing.repository.UserTenantRoleRepository;
import com.neel.security_no_gateway_testing.usercontext.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;

    public Set<Role> getRoles(final Set<String> roleNames) {
        Set<Role> roles = roleRepository.findByNameIn(roleNames);

        if (CollectionUtils.isEmpty(roles) || roles.size() != roleNames.size()) {
            throw new WebsiteException("Invalid Roles");
        }

        return Collections.unmodifiableSet(roles);
    }

    public void updateUserRole(
            final User user,
            final String roleName
    ) {
        final Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Invalid Role Name"));

        UUID tenantGuid = UserContextHolder.getUserContext().tenantGuid();
        Tenant tenant = user.getTenantUserList().stream()
                .map(tenantUser -> tenantUser.getTenant())
                .filter(t -> t.getGuid().equals(tenantGuid))
                .findFirst().get();

        List<UserTenantRole> userTenantRoleList = user.getUserTenantRoles().stream()
                .filter(userTenantRole -> userTenantRole.getTenant().getGuid().equals(tenantGuid))
                .toList();
        userTenantRoleRepository.deleteAll(userTenantRoleList);

        UserTenantRole userTenantRole = UserTenantRole.builder()
                .user(user)
                .tenant(tenant)
                .role(role)
                .build();
        userTenantRoleRepository.save(userTenantRole);
    }
}
