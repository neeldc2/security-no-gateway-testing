package com.neel.security_no_gateway_testing.service;

import com.google.common.collect.Sets;
import com.neel.security_no_gateway_testing.dto.ApproveUserRequest;
import com.neel.security_no_gateway_testing.dto.CreateTenantRequest;
import com.neel.security_no_gateway_testing.dto.CreateTenantResponse;
import com.neel.security_no_gateway_testing.dto.CreateTenantUserRequest;
import com.neel.security_no_gateway_testing.dto.CreateUserRequest;
import com.neel.security_no_gateway_testing.dto.RefreshTokenRequest;
import com.neel.security_no_gateway_testing.dto.RefreshTokenResponse;
import com.neel.security_no_gateway_testing.dto.RejectUserRequest;
import com.neel.security_no_gateway_testing.dto.ResetPasswordRequest;
import com.neel.security_no_gateway_testing.dto.UpdateTenantRequest;
import com.neel.security_no_gateway_testing.dto.UpsertUserProfileRequest;
import com.neel.security_no_gateway_testing.dto.UserInfoResponse;
import com.neel.security_no_gateway_testing.dto.UserLoginRequest;
import com.neel.security_no_gateway_testing.dto.UserLoginResponse;
import com.neel.security_no_gateway_testing.entity.LoginHistory;
import com.neel.security_no_gateway_testing.entity.Permission;
import com.neel.security_no_gateway_testing.entity.Role;
import com.neel.security_no_gateway_testing.entity.RolePermission;
import com.neel.security_no_gateway_testing.entity.Tenant;
import com.neel.security_no_gateway_testing.entity.TenantUser;
import com.neel.security_no_gateway_testing.entity.User;
import com.neel.security_no_gateway_testing.entity.UserProfile;
import com.neel.security_no_gateway_testing.entity.UserTenantRole;
import com.neel.security_no_gateway_testing.enums.AdminActionType;
import com.neel.security_no_gateway_testing.enums.LoginType;
import com.neel.security_no_gateway_testing.enums.UserStatus;
import com.neel.security_no_gateway_testing.exception.WebsiteException;
import com.neel.security_no_gateway_testing.repository.TenantRepository;
import com.neel.security_no_gateway_testing.repository.UserProfileRepository;
import com.neel.security_no_gateway_testing.repository.UserRepository;
import com.neel.security_no_gateway_testing.usercontext.UserContextHolder;
import com.neel.security_no_gateway_testing.utils.ObjectMapperUtils;
import com.neel.security_no_gateway_testing.utils.PasswordValidator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.MANAGE_USERS;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Roles.ADMIN;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Roles.ROLES_ALLOWED_FOR_USER_SIGNUP;
import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Roles.STUDENT;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final RoleService roleService;
    private final AdminActionService adminActionService;
    private final LoginHistoryService loginHistoryService;
    private final TenantUserService tenantUserService;
    private final ObjectMapperUtils objectMapperUtils;
    private final CityService cityService;

    public void userSignUp(final CreateTenantUserRequest createTenantUserRequest) {
        if (!createTenantUserRequest.createUserRequest().active()) {
            throw new RuntimeException("User not active");
        }
        if (createTenantUserRequest.createUserRequest().approved()) {
            throw new RuntimeException("User can only be approved by Admin");
        }

        Set<String> rolesInRequest = createTenantUserRequest.createUserRequest().roleNames();
        if (CollectionUtils.isEmpty(rolesInRequest)) {
            throw new RuntimeException("Roles empty");
        }

        if (!Sets.difference(rolesInRequest, ROLES_ALLOWED_FOR_USER_SIGNUP).isEmpty()) {
            throw new RuntimeException("Roles invalid");
        }

        createTenantUser(createTenantUserRequest);
    }

    public void createTenantUser(final CreateTenantUserRequest createTenantUserRequest) {
        final String password = createTenantUserRequest.createUserRequest().password();
        if (!PasswordValidator.isStrongPassword(password)) {
            throw new WebsiteException("Not a strong password");
        }

        Tenant tenant = getValidTenant(createTenantUserRequest.tenantGuid());

        CreateUserRequest createUserRequest = createTenantUserRequest.createUserRequest();
        Set<Role> roles = roleService.getRoles(createUserRequest.roleNames());
        final Optional<User> userOptional = getUser(createUserRequest.email());
        final User user = userOptional.orElseGet(() -> createTenantUser(createUserRequest));

        final TenantUser tenantUser = TenantUser.builder()
                .tenant(tenant)
                .user(user)
                // default tenant if user is getting created for the first time
                .defaultTenant(userOptional.isEmpty())
                .active(createUserRequest.active())
                .approved(createUserRequest.approved())
                .rejected(false)
                .rejectionCounter(0L)
                .build();
        //tenantUserRepository.save(tenantUser);
        user.addTenantUsers(tenantUser);

        List<UserTenantRole> userTenantRoleList = new ArrayList<>();
        roles.forEach(role -> {
            final UserTenantRole userTenantRole = UserTenantRole.builder()
                    .role(role)
                    .user(user)
                    .tenant(tenant)
                    .build();
            //userTenantRoleRepository.save(userRole);
            userTenantRoleList.add(userTenantRole);
        });
        user.addUserTenantRoles(userTenantRoleList);

        userRepository.save(user);
    }

    public boolean doesUserExists(
            @NonNull final String email) {
        return userRepository.existsByEmail(email);
    }

    public UserLoginResponse userLoginViaUsernamePassword(final UserLoginRequest userLoginRequest,
                                                          final String userAgent,
                                                          final String ipAddress) {
        // UsernamePasswordAuthenticationFilter is the default filter
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginRequest.email(), userLoginRequest.password()));
        // The above code throws BadCredentialsException if username/password is invalid.
        // Below code will not run if username/password is invalid.
        // So, the below if code is not required
        if (authentication.isAuthenticated()) {
            log.info("Good creds");
        }

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        //String jwtToken = jwtService.generateJwtToken(userDetails.getUsername());

        return performUserLogin(
                userLoginRequest.email(),
                userLoginRequest.tenantId(),
                LoginType.USERNAME_PASSWORD,
                userAgent,
                ipAddress);
    }

    public UserLoginResponse performUserLogin(
            @NonNull final String email,
            final Long tenantId,
            final LoginType loginType,
            @NonNull final String userAgent,
            @NonNull final String ipAddress
    ) {
        final User user = getValidUser(email);
        final TenantUser tenantUser = TenantUserService.getValidTenantUser(tenantId, user);
        final String accessToken = getAccessToken(email, tenantUser.getTenant().getId());
        final String refreshToken = jwtService.generateRefreshToken(email, tenantUser.getTenant().getId());

        // if user was imported by admin, then the status would be inactive.
        // when the user logs in for the first time, change the status to active
        if (!tenantUser.isActive()) {
            tenantUserService.activateTenantUser(List.of(tenantUser));
        }

        LoginHistory loginHistory = LoginHistory.builder()
                .userId(user.getId())
                .tenantId(tenantUser.getTenant().getId())
                .email(user.getEmail())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .loginType(loginType)
                .loginTimestamp(Instant.now())
                .build();
        loginHistoryService.saveLoginHistory(loginHistory);

        return UserLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void captureFailedUserLoginHistory(final UserLoginRequest userLoginRequest,
                                              final LoginType loginType,
                                              final String userAgent,
                                              final String ipAddress,
                                              final Exception exception) {
        LoginHistory loginHistory = LoginHistory.builder()
                .email(userLoginRequest.email())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(false)
                .loginType(loginType)
                .failureReason(exception.getMessage())
                .loginTimestamp(Instant.now())
                .build();
        loginHistoryService.saveLoginHistory(loginHistory);
    }

    public RefreshTokenResponse refreshToken(
            final RefreshTokenRequest refreshTokenRequest) {
        final String email = jwtService.getSubject(refreshTokenRequest.refreshToken());
        final Long tenantId = jwtService.getTenantId(refreshTokenRequest.refreshToken());
        final String accessToken = getAccessToken(email, tenantId);

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    public CreateTenantResponse registerTenant(final CreateTenantRequest createTenantRequest) {
        validateTenantRequest(createTenantRequest);

        // Create Tenant
        final Tenant tenant = Tenant.builder()
                .guid(UUID.randomUUID())
                .name(createTenantRequest.tenantName())
                .databaseName(createTenantRequest.tenantCode())
                .build();
        tenantRepository.save(tenant);

        // Create first user in that tenant
        final CreateTenantUserRequest createTenantUserRequest = CreateTenantUserRequest.builder()
                .tenantGuid(tenant.getGuid())
                .createUserRequest(createTenantRequest.createUserRequest())
                .build();
        createTenantUser(createTenantUserRequest);

        CreateTenantResponse createTenantResponse = CreateTenantResponse.builder()
                .tenantId(tenant.getId())
                .tenantGuid(tenant.getGuid())
                .build();

        notificationService.sendEmailOnAddTenant(
                createTenantUserRequest.createUserRequest().email(),
                tenant.getId()
        );

        cityService.importCitiesFromCSV();

        return createTenantResponse;
    }

    public void updateTenant(final UpdateTenantRequest updateTenantRequest) {
        final Tenant tenant = getTenant(updateTenantRequest.tenantGuid());

        final boolean currentStatus = tenant.isEnabled();
        final boolean newStatus = updateTenantRequest.enable();
        if (currentStatus == newStatus) {
            return;
        }

        tenant.setEnabled(newStatus);
        tenantRepository.save(tenant);
    }

    public void upsertUserProfile(final UpsertUserProfileRequest upsertUserProfileRequest) {
        User user = getUser(upsertUserProfileRequest.email())
                .orElseThrow(() -> new WebsiteException("User does not exists"));

        UUID userIdFromContext = UserContextHolder.getUserContext().userId();
        Set<String> userPermissions = UserContextHolder.getUserContext().permissions();
        UUID userId = user.getId();

        // Users can update only their own profile
        // Only admins can update other user profiles
        if (!userId.equals(userIdFromContext) &&
                !userPermissions.contains(MANAGE_USERS)) {
            throw new WebsiteException("User does not have permission to update profile");
        }

        Long tenantId = UserContextHolder.getUserContext().tenantId();
        Optional<UserProfile> userProfileOptional = getUserProfile(userId, tenantId);
        UserProfile userProfile;

        if (userProfileOptional.isEmpty()) {
            userProfile = new UserProfile();
            userProfile.setUserId(userId);
            userProfile.setTenantId(tenantId);
        } else {
            userProfile = userProfileOptional.get();
        }

        userProfile.setFirstName(upsertUserProfileRequest.firstName());
        userProfile.setMiddleName(upsertUserProfileRequest.middleName());
        userProfile.setLastName(upsertUserProfileRequest.lastName());
        userProfile.setGender(upsertUserProfileRequest.gender());
        userProfile.setUsn(upsertUserProfileRequest.usn());
        userProfile.setYearOfAdmission(upsertUserProfileRequest.yearOfAdmission());
        userProfile.setYearOfPassing(upsertUserProfileRequest.yearOfPassing());
        userProfile.setPhoneNumber(upsertUserProfileRequest.phoneNumber());
        userProfile.setGender(upsertUserProfileRequest.gender());
        userProfile.setBranch(upsertUserProfileRequest.branch());

        userProfileRepository.save(userProfile);
    }

    public List<UserInfoResponse> getAllUsersResponseInTenant(
            final UserStatus userStatus
    ) {
        Long tenantId = UserContextHolder.getUserContext().tenantId();

        // There cannot be a scenario where both are true.
        boolean rejected;
        boolean approved;

        switch (userStatus) {
            case APPROVED:
                approved = true;
                rejected = false;
                break;

            case REJECTED:
                approved = false;
                rejected = true;
                break;

            case APPROVAL_PENDING:
                approved = false;
                rejected = false;
                break;

            default:
                throw new WebsiteException("Unsupported Type");
        }

        List<User> users = getAllUsers(approved, rejected);

        // TODO: Get User Profile from Another microservice
        return users.stream()
                .map(user ->
                        TenantUserService
                                .getTenantUser(tenantId, user)
                                .map(tenantUser -> UserInfoResponse.getUserInfoResponse(user, tenantUser))
                                .orElse(null)
                )
                .filter(Predicate.not(Objects::isNull))
                .toList();
    }

    public void approveUsers(final List<ApproveUserRequest> approveUserRequestList,
                             final String ipAddress,
                             final String userAgent) {
        Long tenantId = UserContextHolder.getUserContext().tenantId();

        final Set<UUID> userIds = approveUserRequestList.stream()
                .map(ApproveUserRequest::userId)
                .collect(Collectors.toSet());

        final List<User> users = userRepository.findByIdInLeftJoinFetchTenantAndUserTenantRoles(userIds);
        final List<TenantUser> tenantUserList = users.stream()
                .map(user -> TenantUserService.getTenantUser(tenantId, user).get())
                .toList();
        tenantUserService.approveTenantUsers(tenantUserList);

        // Upgrade these users role from USERS to STUDENT
        users.forEach(user -> roleService.updateUserRole(user, STUDENT));

        adminActionService.addAdminAction(
                AdminActionType.APPROVE_USERS,
                objectMapperUtils.getObjectAsString(userIds),
                ipAddress,
                userAgent
        );
    }

    public void rejectUsers(final List<RejectUserRequest> rejectUserRequestList,
                            final String ipAddress,
                            final String userAgent
    ) {
        Long tenantId = UserContextHolder.getUserContext().tenantId();

        final Set<UUID> userIds = rejectUserRequestList.stream()
                .map(RejectUserRequest::userId)
                .collect(Collectors.toSet());

        final List<User> users = userRepository.findByIdInLeftJoinFetchTenantAndUserTenantRoles(userIds);
        final List<TenantUser> tenantUserList = users.stream()
                .map(user -> TenantUserService.getTenantUser(tenantId, user).get())
                .toList();
        tenantUserService.rejectTenantUsers(tenantUserList);

        adminActionService.addAdminAction(
                AdminActionType.REJECT_USERS,
                objectMapperUtils.getObjectAsString(userIds),
                ipAddress,
                userAgent
        );
    }

    public void addAdmin(final UUID newAdminUserId,
                         final String ipAddress,
                         final String userAgent) {
        if (!doesUserExistsWithSameTenant(newAdminUserId)) {
            throw new RuntimeException("user does not exists");
        }

        Long tenantId = UserContextHolder.getUserContext().tenantId();

        final User user = userRepository.findByIdInLeftJoinFetchTenantAndUserTenantRoles(Set.of(newAdminUserId)).get(0);
        final TenantUser tenantUser = TenantUserService.getValidTenantUser(tenantId, user);
        if (!user.isEnabled()) {
            throw new WebsiteException("User should be enabled");
        }
        if (!tenantUser.isActive()) {
            throw new WebsiteException("User has to login at-least once");
        }
        if (!tenantUser.isApproved()) {
            throw new WebsiteException("User is not approved");
        }

        roleService.updateUserRole(user, ADMIN);

        adminActionService.addAdminAction(
                AdminActionType.ADD_ADMIN,
                objectMapperUtils.getObjectAsString(newAdminUserId),
                ipAddress,
                userAgent
        );
    }

    public void requestApproval() {
        Long tenantId = UserContextHolder.getUserContext().tenantId();
        UUID userId = UserContextHolder.getUserContext().userId();

        User user = userRepository.findByTenantIdLeftJoinFetchTenantUser(userId, tenantId);
        final TenantUser tenantUser = TenantUserService.getValidTenantUser(tenantId, user);

        if (tenantUser.isApproved()) {
            throw new WebsiteException("User already approved");
        }

        if (tenantUser.getRejectionCounter() >= 2) {
            throw new WebsiteException("Max limit reached");
        }

        tenantUserService.requestAdminApproval(tenantUser);
    }

    private List<User> getAllUsers(
            final boolean approved,
            final boolean rejected
    ) {
        Long currentUserTenantId = UserContextHolder.getUserContext().tenantId();
        return userRepository.findByTenantIdAndApprovedAndRejected(currentUserTenantId, approved, rejected);
    }

    private List<User> getAllUsers() {
        Long currentUserTenantId = UserContextHolder.getUserContext().tenantId();
        return userRepository.findByTenantIdLeftJoinFetchTenantUser(currentUserTenantId);
    }

    public void deleteUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> {
            if (user.getEmail().contains("neel") || user.getEmail().contains("prem")) {
                return;
            }
            userRepository.delete(user);
        });
    }

    public void sendResetPasswordEmail(
            final String email) {
        final User user = getValidUser(email);
        final TenantUser tenantUser = TenantUserService.getValidTenantUser(null, user);

        notificationService.sendEmailOnAddTenant(
                email,
                tenantUser.getTenant().getId()
        );
    }

    public void resetPassword(final ResetPasswordRequest resetPasswordRequest) {
        final String refreshToken = resetPasswordRequest.refreshToken();
        final String email = jwtService.getSubject(refreshToken);
        final Long tenantId = jwtService.getTenantId(refreshToken);

        final User user = getValidUser(email);
        final TenantUser tenantUser = TenantUserService.getValidTenantUser(tenantId, user);

        String password = resetPasswordRequest.password();
        if (!PasswordValidator.isStrongPassword(password)) {
            throw new WebsiteException("Not a strong password");
        }

        user.setPassword(encoder.encode(resetPasswordRequest.password()));
        userRepository.save(user);
    }

    public boolean doesUserExistsWithSameTenant(
            final String email
    ) {
        Optional<User> userOptional = getUser(email);
        return doesUserExistsWithSameTenant(userOptional);
    }

    public boolean doesUserExistsWithSameTenant(
            final UUID userId
    ) {
        Optional<User> userOptional = getUser(userId);
        return doesUserExistsWithSameTenant(userOptional);
    }

    private static boolean doesUserExistsWithSameTenant(final Optional<User> userOptional) {
        if (userOptional.isEmpty()) {
            return false;
        }

        Long tenantId = UserContextHolder.getUserContext().tenantId();
        if (TenantUserService.getTenantUser(tenantId, userOptional.get()).isEmpty()) {
            return false;
        }
        return true;
    }

    private User getValidUser(String email) {
        return getUser(email)
                .orElseThrow(() -> new WebsiteException("Invalid Email"));
    }

    private String getAccessToken(
            @NonNull final String email,
            @NonNull final Long tenantId
    ) {
        User user = getUser(email).get();
        TenantUser tenantUser = user.getTenantUserList().stream()
                .filter(tenantUserToFilter -> tenantUserToFilter.getTenant().getId().equals(tenantId))
                .findFirst()
                .get();
        Tenant tenant = tenantUser.getTenant();

        Set<String> permissions = user.getUserTenantRoles().stream()
                .filter(userTenantRole -> userTenantRole.getTenant().getGuid().equals(tenant.getGuid()))
                .map(UserTenantRole::getRole)
                .map(Role::getRolePermissionList)
                .flatMap(List::stream)
                .map(RolePermission::getPermission)
                .map(Permission::getName)
                .collect(Collectors.toSet());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tenantId", tenant.getId());
        claims.put("tenantGuid", tenant.getGuid());
        claims.put("tenant", tenant.getDatabaseName());
        claims.put("permissions", permissions);

        return jwtService.generateJwtToken(email, claims);
    }

    private User createTenantUser(CreateUserRequest createUserRequest) {
        final User newUser = User.builder()
                .email(createUserRequest.email())
                .enabled(true)
                .username(createUserRequest.username())
                // BCryptPasswordEncoder uses salt by default
                .password(encoder.encode(createUserRequest.password()))
                .build();
        //return userRepository.save(newUser);
        return newUser;
    }

    private Tenant getTenant(final UUID tenantGuid) {
        final Tenant tenant = tenantRepository.findByGuid(tenantGuid)
                .orElseThrow(() -> new RuntimeException("Tenant Guid is invalid"));
        return tenant;
    }

    private Tenant getValidTenant(final UUID tenantGuid) {
        final Tenant tenant = tenantRepository.findByGuid(tenantGuid)
                .orElseThrow(() -> new RuntimeException("Tenant Guid is invalid"));

        if (!tenant.isEnabled()) {
            throw new WebsiteException("Tenant is disabled");
        }
        return tenant;
    }

    private void validateTenantRequest(final CreateTenantRequest createTenantRequest) {
        final Tenant tenant = tenantRepository.findByName(createTenantRequest.tenantName());

        if (tenant != null) {
            throw new WebsiteException("Tenant exists already");
        }

        CreateUserRequest createUserRequest = createTenantRequest.createUserRequest();
        if (!createUserRequest.active()) {
            throw new RuntimeException("Tenant Admin should be enabled");
        }
        if (!createUserRequest.approved()) {
            throw new RuntimeException("Tenant Admin should be approved");
        }

        Set<String> roleNamesInRequest = createUserRequest.roleNames();
        if (!Set.of(ADMIN).equals(roleNamesInRequest)) {
            throw new RuntimeException("Tenant User is not admin");
        }
    }

    private Optional<User> getUser(final String email) {
        return userRepository.findByEmail(email);
    }

    private Optional<User> getUser(final UUID userId) {
        return userRepository.findById(userId);
    }

    private Optional<UserProfile> getUserProfile(UUID userId, Long tenantId) {
        return userProfileRepository.findByUserIdAndTenantId(userId, tenantId);
    }

}
