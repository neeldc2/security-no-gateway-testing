package com.neel.security_no_gateway_testing.dto;

import lombok.NonNull;

import java.util.UUID;

public record UpdateTenantRequest(
        @NonNull UUID tenantGuid,
        @NonNull boolean enable
) {
}
