package com.neel.security_no_gateway_testing.controller;

import com.neel.security_no_gateway_testing.dto.CreateTenantUserRequest;
import com.neel.security_no_gateway_testing.dto.RefreshTokenRequest;
import com.neel.security_no_gateway_testing.dto.RefreshTokenResponse;
import com.neel.security_no_gateway_testing.dto.ResetPasswordRequest;
import com.neel.security_no_gateway_testing.dto.TenantInfoResponse;
import com.neel.security_no_gateway_testing.dto.UserLoginRequest;
import com.neel.security_no_gateway_testing.dto.UserLoginResponse;
import com.neel.security_no_gateway_testing.dto.ValidateResponse;
import com.neel.security_no_gateway_testing.dto.ValidationRequest;
import com.neel.security_no_gateway_testing.exception.WebsiteException;
import com.neel.security_no_gateway_testing.service.JwtService;
import com.neel.security_no_gateway_testing.service.TenantService;
import com.neel.security_no_gateway_testing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.neel.security_no_gateway_testing.enums.LoginType.USERNAME_PASSWORD;

@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginAndRegistrationController {

    private final UserService userService;
    private final TenantService tenantService;
    private final JwtService jwtService;

    @PostMapping("/user")
    public void createTenantUser(@RequestBody CreateTenantUserRequest createTenantUserRequest) {
        userService.userSignUp(createTenantUserRequest);
    }

    @PostMapping("/login")
    public UserLoginResponse userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                       @RequestHeader("User-Agent") String userAgent,
                                       @RequestHeader("Ip-Address") String ipAddress) {
        try {
            return userService.userLoginViaUsernamePassword(userLoginRequest, userAgent, ipAddress);
        } catch (Exception exception) {
            userService.captureFailedUserLoginHistory(userLoginRequest, USERNAME_PASSWORD, userAgent, ipAddress, exception);
            throw exception;
        }
    }

    @PostMapping("/refresh")
    public RefreshTokenResponse userLogin(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return userService.refreshToken(refreshTokenRequest);
    }

    @GetMapping("/tenants")
    public Set<TenantInfoResponse> getTenantInfoList() {
        return tenantService.getRegisteredTenants();
    }

    /**
     * Audit wont be needed if Email service captures all the emails sent
     *
     * @param email
     */
    @PostMapping("/reset-password-email")
    public void sendResetPasswordEmail(
            @RequestParam String email
    ) {
        userService.sendResetPasswordEmail(email);
    }

    /**
     * Audit wont be needed if Email service captures all the emails sent
     *
     * @param resetPasswordRequest
     */
    @PutMapping("/reset-password")
    public void resetPassword(
            @RequestBody ResetPasswordRequest resetPasswordRequest
    ) {
        userService.resetPassword(resetPasswordRequest);
    }

    @PostMapping("/validate")
    public ValidateResponse validateToken(@RequestBody ValidationRequest validationRequest) {
        final String authorisationHeader = validationRequest.authorizationToken();
        if (StringUtils.hasText(authorisationHeader) && authorisationHeader.startsWith("Bearer ")) {

            String token = authorisationHeader.substring(7);

            return jwtService.getValidateResponse(token);
        }

        throw new WebsiteException("No Header");
    }

}
