package com.neel.security_no_gateway_testing.dto;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record UpsertUserProfileRequest(
        @NonNull String firstName,
        String middleName,
        String lastName,
        @NonNull String email,
        String gender,
        String usn,
        Integer yearOfAdmission,
        Integer yearOfPassing,
        String phoneNumber,
        String branch
) {
}
