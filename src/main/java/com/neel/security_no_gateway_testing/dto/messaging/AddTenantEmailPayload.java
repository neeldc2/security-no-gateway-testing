package com.neel.security_no_gateway_testing.dto.messaging;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@ToString
@NoArgsConstructor
public class AddTenantEmailPayload extends EmailPayload {
    @NonNull
    private String urlToResetPassword;
}
