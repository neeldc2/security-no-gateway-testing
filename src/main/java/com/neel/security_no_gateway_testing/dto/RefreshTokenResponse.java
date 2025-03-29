package com.neel.security_no_gateway_testing.dto;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record RefreshTokenResponse(
        @NonNull String accessToken
) {
}
