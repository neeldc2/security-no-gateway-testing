package com.neel.security_no_gateway_testing.dto;

import lombok.NonNull;

import java.util.UUID;

public record OAuthLoginAuthCode(
        @NonNull String code,
        @NonNull String redirectURI,
        @NonNull Long tenantId,
        @NonNull UUID tenantGuid
) {
}
