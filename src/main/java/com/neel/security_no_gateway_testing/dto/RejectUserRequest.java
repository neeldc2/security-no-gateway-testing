package com.neel.security_no_gateway_testing.dto;

import java.util.UUID;

public record RejectUserRequest(
        UUID userId
) {
}
