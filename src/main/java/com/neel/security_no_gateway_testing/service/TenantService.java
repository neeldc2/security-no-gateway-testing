package com.neel.security_no_gateway_testing.service;

import com.neel.security_no_gateway_testing.dto.TenantInfoResponse;
import com.neel.security_no_gateway_testing.entity.Tenant;
import com.neel.security_no_gateway_testing.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Transactional
@Slf4j
@RequiredArgsConstructor
@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public Set<TenantInfoResponse> getRegisteredTenants() {
        return tenantRepository.findAll().stream()
                .filter(Predicate.not(Tenant::isTestingTenant))
                .filter(Tenant::isEnabled)
                .map(TenantInfoResponse::getTenantInfoResponse)
                .collect(Collectors.toSet());
    }
}
