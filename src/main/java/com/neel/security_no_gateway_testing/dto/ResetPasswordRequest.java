package com.neel.security_no_gateway_testing.dto;

public record ResetPasswordRequest(
        String password,
        String refreshToken
) {
}
