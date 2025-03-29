package com.neel.security_no_gateway_testing.controller;

import com.neel.security_no_gateway_testing.annotation.ValidatePermission;
import com.neel.security_no_gateway_testing.dto.ApproveUserRequest;
import com.neel.security_no_gateway_testing.dto.CreateTenantRequest;
import com.neel.security_no_gateway_testing.dto.CreateTenantResponse;
import com.neel.security_no_gateway_testing.dto.RejectUserRequest;
import com.neel.security_no_gateway_testing.dto.UpdateTenantRequest;
import com.neel.security_no_gateway_testing.dto.UpsertUserProfileRequest;
import com.neel.security_no_gateway_testing.dto.UserInfoResponse;
import com.neel.security_no_gateway_testing.enums.UserStatus;
import com.neel.security_no_gateway_testing.exception.WebsiteException;
import com.neel.security_no_gateway_testing.service.NotificationService;
import com.neel.security_no_gateway_testing.service.PermissionService;
import com.neel.security_no_gateway_testing.service.UserService;
import com.neel.security_no_gateway_testing.usercontext.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.ADD_ADMIN;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.APPROVED_USER;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.EDIT_PROFILE;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.MANAGE_TENANT;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.MANAGE_USERS;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthenticatedController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final NotificationService notificationService;

    // This is also for basic auth
    // This is to test APIs using JWT
    @GetMapping("/test2")
    @ValidatePermission({"MANAGE_USERS"})
    public String testAPI() {
        log.info("User is {}", UserContextHolder.getUserContext().userId());
        return "works";
    }

    /**
     * Create first tenant via SQL script. Add yourself as a user with MANAGE_TENANT permission.
     * Script is present in add_super_user.sql file
     * Then, you would be able to add tenants.
     *
     * @param createTenantRequest
     * @return
     */
    //@ValidatePermission({"MANAGE_TENANT"})
    @PostMapping("/tenants")
    public CreateTenantResponse registerTenant(@RequestBody CreateTenantRequest createTenantRequest) {
        return userService.registerTenant(createTenantRequest);
    }

    @ValidatePermission({MANAGE_TENANT})
    @PutMapping("/tenants")
    public void updateTenant(@RequestBody UpdateTenantRequest updateTenantRequest) {
        userService.updateTenant(updateTenantRequest);
    }

    @ValidatePermission({EDIT_PROFILE})
    @PutMapping("/user-profile")
    public void updateUserProfile(@RequestBody UpsertUserProfileRequest upsertUserProfileRequest) {
        userService.upsertUserProfile(upsertUserProfileRequest);
    }

    // TODO: order by name. Add pagination
    @ValidatePermission({MANAGE_USERS})
    @GetMapping("/users")
    public List<UserInfoResponse> getAllUsersInTenant(
            @RequestParam UserStatus userStatus
    ) {
        return userService.getAllUsersResponseInTenant(userStatus);
    }

    @ValidatePermission({MANAGE_USERS})
    @PostMapping("/users/approve")
    public void approveUsers(@RequestBody List<ApproveUserRequest> approveUserRequestList,
                             @RequestHeader("Ip-Address") String ipAddress,
                             @RequestHeader("User-Agent") String userAgent
    ) {
        userService.approveUsers(approveUserRequestList, ipAddress, userAgent);
    }

    @ValidatePermission({MANAGE_USERS})
    @PostMapping("/users/reject")
    public void rejectUsers(@RequestBody List<RejectUserRequest> rejectUserRequestList,
                            @RequestHeader("Ip-Address") String ipAddress,
                            @RequestHeader("User-Agent") String userAgent
    ) {
        userService.rejectUsers(rejectUserRequestList, ipAddress, userAgent);
    }

    @ValidatePermission({ADD_ADMIN})
    @PostMapping("/users/admin")
    public void addAdminUser(@RequestParam UUID newAdminUserId,
                             @RequestHeader("Ip-Address") String ipAddress,
                             @RequestHeader("User-Agent") String userAgent
    ) {
        userService.addAdmin(newAdminUserId, ipAddress, userAgent);
    }

    @ValidatePermission({EDIT_PROFILE})
    @PostMapping("/users/request-approval")
    public void requestApproval() {
        userService.requestApproval();
    }

    @GetMapping("/permissions")
    public Set<String> getPermission() {
        return permissionService.getPermissions();
    }

    @ValidatePermission({MANAGE_USERS})
    @DeleteMapping("/users")
    public void deleteAllUsers() {
        userService.deleteUsers();
    }

    @ValidatePermission({APPROVED_USER})
    @PostMapping("/users/invite")
    public void inviteUser(
            @RequestParam String email
    ) {
        if (userService.doesUserExistsWithSameTenant(email)) {
            throw new WebsiteException("User Already exists within same tenant");
        }
        notificationService.inviteNewUserToTenant(email);
    }

}
