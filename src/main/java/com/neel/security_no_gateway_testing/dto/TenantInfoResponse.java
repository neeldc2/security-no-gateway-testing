package com.neel.security_no_gateway_testing.dto;

import com.neel.security_no_gateway_testing.entity.Tenant;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TenantInfoResponse(
        UUID tenantGuid,
        Long tenantId,
        String tenantName
) {

    public static TenantInfoResponse getTenantInfoResponse(
            final Tenant tenant
    ) {
        return TenantInfoResponse.builder()
                .tenantGuid(tenant.getGuid())
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .build();
    }
}
