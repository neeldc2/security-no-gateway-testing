package com.neel.security_no_gateway_testing.dto;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder
public record CreateTenantUserRequest(
        @NonNull UUID tenantGuid,
        @NonNull CreateUserRequest createUserRequest
) {
}
