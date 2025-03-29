package com.neel.security_no_gateway_testing.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateTenantResponse(
        Long tenantId,
        UUID tenantGuid
) {
}
