package com.neel.security_no_gateway_testing.aspect;

import com.neel.security_no_gateway_testing.annotation.ValidatePermission;
import com.neel.security_no_gateway_testing.exception.WebsiteException;
import com.neel.security_no_gateway_testing.usercontext.UserContext;
import com.neel.security_no_gateway_testing.usercontext.UserContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Aspect
@Component
public class PermissionValidationAspect {

    @Pointcut("@annotation(com.neel.security_no_gateway_testing.annotation.ValidatePermission)  || @within(com.neel.security_no_gateway_testing.annotation.ValidatePermission)")
    public void validatePermissionPointcut() {
        // Pointcut for methods annotated with @ValidatePermission
    }

    @Around("validatePermissionPointcut()")
    public Object validatePermissions(ProceedingJoinPoint joinPoint) throws Throwable {
        UserContext userContext = UserContextHolder.getUserContext();
        Set<String> userPermissions = userContext.permissions();

        // Retrieve required permissions from the annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ValidatePermission methodPermission = method.getAnnotation(ValidatePermission.class);
        List<String> methodLevelPermission = methodPermission != null ?
                Arrays.stream(methodPermission.value()).toList() : List.of();

        // Get required permissions from the class-level annotation
        Class<?> targetClass = method.getDeclaringClass();
        ValidatePermission classPermission = targetClass.getAnnotation(ValidatePermission.class);
        List<String> classLevelPermission = classPermission != null ?
                Arrays.stream(classPermission.value()).toList() :
                List.of();

        if (!CollectionUtils.isEmpty(methodLevelPermission)) {
            boolean hasPermission = methodLevelPermission.stream()
                    .allMatch(userPermissions::contains);

            if (!hasPermission) {
                throw new WebsiteException("Access Denied: Insufficient permissions");
            }
        } else if (!CollectionUtils.isEmpty(classLevelPermission)) {
            boolean hasPermission = classLevelPermission.stream()
                    .allMatch(userPermissions::contains);

            if (!hasPermission) {
                throw new WebsiteException("Access Denied: Insufficient permissions");
            }
        }

        return joinPoint.proceed();
    }
}
