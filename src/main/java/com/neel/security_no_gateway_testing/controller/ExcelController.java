package com.neel.security_no_gateway_testing.controller;

import com.neel.security_no_gateway_testing.annotation.ValidatePermission;
import com.neel.security_no_gateway_testing.dto.UserProfileExcelDto;
import com.neel.security_no_gateway_testing.enums.AdminActionType;
import com.neel.security_no_gateway_testing.service.AdminActionService;
import com.neel.security_no_gateway_testing.service.UserImportService;
import com.neel.security_no_gateway_testing.utils.ExcelUtils;
import com.neel.security_no_gateway_testing.utils.ObjectMapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.Permissions.MANAGE_USERS;


@RestController
@RequiredArgsConstructor
@Slf4j
public class ExcelController {

    private final UserImportService userImportService;
    private final AdminActionService adminActionService;
    private final ObjectMapperUtils objectMapperUtils;

    @ValidatePermission({MANAGE_USERS})
    @PostMapping("/import")
    public List<UserProfileExcelDto> uploadExcel(@RequestParam("file") MultipartFile file,
                                                 @RequestHeader("Ip-Address") String ipAddress,
                                                 @RequestHeader("User-Agent") String userAgent
    ) {
        try {
            List<UserProfileExcelDto> userProfileExcelDtoList = ExcelUtils.readExcelFile(file);
            userProfileExcelDtoList.forEach(userProfileExcelDto ->
                    userImportService.importUser(userProfileExcelDto));
            // TODO: send email to reset their password

            List<String> emailIds = userProfileExcelDtoList.stream()
                    .map(UserProfileExcelDto::email)
                    .collect(Collectors.toList());
            adminActionService.addAdminAction(
                    AdminActionType.IMPORT_USERS_VIA_FILE,
                    objectMapperUtils.getObjectAsString(emailIds),
                    ipAddress,
                    userAgent
            );
            //userImportService.importUsers(userProfileExcelDtoList);
            return userProfileExcelDtoList;
        } catch (IOException e) {
            throw new RuntimeException("Exception importing users");
        }
    }

    @ValidatePermission({MANAGE_USERS})
    @PostMapping("/import-2")
    public List<UserProfileExcelDto> uploadExcel2(@RequestParam("file") MultipartFile file,
                                                  @RequestHeader("Ip-Address") String ipAddress,
                                                  @RequestHeader("User-Agent") String userAgent
    ) {
        try {
            List<CompletableFuture<String>> futures = new ArrayList<>();
            List<UserProfileExcelDto> userProfileExcelDtoList = ExcelUtils.readExcelFile(file);
            userProfileExcelDtoList.forEach(userProfileExcelDto -> futures.add(userImportService.importUser2(userProfileExcelDto)));

            // Wait for all tasks to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            // Block until all tasks are complete
            allOf.join();

            List<String> emailIds = userProfileExcelDtoList.stream()
                    .map(UserProfileExcelDto::email)
                    .collect(Collectors.toList());
            adminActionService.addAdminAction(
                    AdminActionType.IMPORT_USERS_VIA_FILE,
                    objectMapperUtils.getObjectAsString(emailIds),
                    ipAddress,
                    userAgent
            );

            return userProfileExcelDtoList;
        } catch (IOException e) {
            throw new RuntimeException("Exception importing users");
        }

    }
}
