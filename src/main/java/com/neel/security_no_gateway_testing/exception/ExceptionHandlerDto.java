package com.neel.security_no_gateway_testing.exception;

import lombok.Builder;

@Builder
public record ExceptionHandlerDto(
        String message
) {
}
