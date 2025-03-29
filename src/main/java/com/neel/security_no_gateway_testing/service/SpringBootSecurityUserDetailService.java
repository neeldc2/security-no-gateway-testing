package com.neel.security_no_gateway_testing.service;

import com.neel.security_no_gateway_testing.entity.User;
import com.neel.security_no_gateway_testing.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class SpringBootSecurityUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    public SpringBootSecurityUserDetailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        /*Set<GrantedAuthority> authorities = user.getUserRoleList()
                .stream()
                .map(UserTenantRole::getRole)
                .flatMap(role -> role.getRolePermissionList().stream().map(RolePermission::getPermission))
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toSet());*/

        // return UserPrincipal from 1.36
        return new org.springframework.security.core.userdetails.User(
                //user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                Set.of()
        );
    }

}
