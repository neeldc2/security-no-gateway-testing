package com.neel.security_no_gateway_testing.dto;

import com.neel.security_no_gateway_testing.entity.TenantUser;
import com.neel.security_no_gateway_testing.entity.User;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserInfoResponse(
        UUID userId,
        String email,
        String firstName,
        String middleName,
        String lastName,
        boolean active,
        boolean approved,
        Long rejectionCounter
) {
    public static UserInfoResponse getUserInfoResponse(
            final User user,
            final TenantUser tenantUser
    ) {
        return UserInfoResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .active(tenantUser.isActive())
                .approved(tenantUser.isApproved())
                .rejectionCounter(tenantUser.getRejectionCounter())
                .build();
    }
}
