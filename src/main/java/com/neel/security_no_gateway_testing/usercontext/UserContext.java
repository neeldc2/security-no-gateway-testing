package com.neel.security_no_gateway_testing.usercontext;

import lombok.Builder;
import lombok.NonNull;

import java.util.Set;
import java.util.UUID;

@Builder
public record UserContext(
        @NonNull UUID userId,
        @NonNull Long tenantId,
        @NonNull UUID tenantGuid,
        @NonNull String tenant,
        @NonNull Set<String> permissions
) {
}
