package com.neel.security_no_gateway_testing.service;

import com.neel.security_no_gateway_testing.dto.CreateTenantUserRequest;
import com.neel.security_no_gateway_testing.dto.CreateUserRequest;
import com.neel.security_no_gateway_testing.dto.UpsertUserProfileRequest;
import com.neel.security_no_gateway_testing.dto.UserProfileExcelDto;
import com.neel.security_no_gateway_testing.usercontext.UserContextHolder;
import com.neel.security_no_gateway_testing.utils.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Roles.STUDENT;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserImportService {

    private final UserService userService;

    public void importUsers(final List<UserProfileExcelDto> userProfileExcelDtoList) {
        userProfileExcelDtoList.forEach(this::importUser);
    }

    @Async("threadPoolTaskExecutor")
    public void importUser(final UserProfileExcelDto userProfileExcelDto
    ) {
        UUID tenantGuid = UserContextHolder.getUserContext().tenantGuid();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .email(userProfileExcelDto.email())
                .username(userProfileExcelDto.firstName())
                .password(PasswordGenerator.generateSecurePassword(10))
                .active(false)
                .approved(true)
                .roleNames(Set.of(STUDENT))
                .build();

        log.info("email: {}", createUserRequest.email());
        log.info("password: {}", createUserRequest.password());

        CreateTenantUserRequest createTenantUserRequest = CreateTenantUserRequest.builder()
                .tenantGuid(tenantGuid)
                .createUserRequest(createUserRequest)
                .build();
        userService.createTenantUser(createTenantUserRequest);

        UpsertUserProfileRequest upsertUserProfileRequest = UpsertUserProfileRequest.builder()
                .firstName(userProfileExcelDto.firstName())
                .middleName(userProfileExcelDto.middleName())
                .lastName(userProfileExcelDto.lastName())
                .email(userProfileExcelDto.email())
                .gender(userProfileExcelDto.gender())
                .usn(userProfileExcelDto.usn())
                .yearOfAdmission(userProfileExcelDto.yearOfAdmission())
                .yearOfPassing(userProfileExcelDto.yearOfPassing())
                .phoneNumber(userProfileExcelDto.phoneNumber())
                .branch(userProfileExcelDto.branch())
                .build();
        userService.upsertUserProfile(upsertUserProfileRequest);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<String> importUser2(UserProfileExcelDto userProfileExcelDto) {
        importUser(userProfileExcelDto);
        return CompletableFuture.completedFuture("Completed");
    }

}
