package com.neel.security_no_gateway_testing.dto;

import lombok.NonNull;

public record UserLoginRequest(
        @NonNull String email,
        @NonNull String password,
        Long tenantId // needed only for multi-tenant user
) {
}
