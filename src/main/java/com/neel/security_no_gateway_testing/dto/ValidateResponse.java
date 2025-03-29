package com.neel.security_no_gateway_testing.dto;

import com.neel.security_no_gateway_testing.usercontext.UserContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ValidateResponse {
    private UserContext userContext;
    private Long expiryTime;
}
