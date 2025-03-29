package com.neel.security_no_gateway_testing.dto;

import lombok.NonNull;

public record CreateTenantRequest(
        @NonNull String tenantName,
        @NonNull String tenantCode,
        CreateUserRequest createUserRequest
) {
}
