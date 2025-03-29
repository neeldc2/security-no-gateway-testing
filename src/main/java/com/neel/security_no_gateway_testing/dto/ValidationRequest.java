package com.neel.security_no_gateway_testing.dto;

import lombok.NonNull;

public record ValidationRequest(
        @NonNull String authorizationToken
) {
}
