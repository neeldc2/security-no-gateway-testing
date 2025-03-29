package com.neel.security_no_gateway_testing.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@AllArgsConstructor
@Data
@SuperBuilder
@NoArgsConstructor
public class EmailPayload {
    @NonNull
    private Set<String> toEmailIds;
    private Set<String> ccEmailIds;
    private Set<String> bccEmailIds;
    @NonNull
    private EmailType emailType;
}
