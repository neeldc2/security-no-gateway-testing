package com.neel.security_no_gateway_testing.filter;

import com.neel.security_no_gateway_testing.entity.User;
import com.neel.security_no_gateway_testing.repository.UserRepository;
import com.neel.security_no_gateway_testing.service.JwtService;
import com.neel.security_no_gateway_testing.service.SpringBootSecurityUserDetailService;
import com.neel.security_no_gateway_testing.usercontext.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.neel.security_no_gateway_testing.constant.WebsiteLoginConstants.USER_CONTEXT_ATTRIBUTE;

/**
 * Explanation on difference between Filter and Interceptor
 * https://stackoverflow.com/questions/35856454/difference-between-interceptor-and-filter-in-spring-mvc
 */
@Component
public class AuthenticationJwtTokenFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION = "Authorization";
    public static final String X_SERVICE = "x-service";
    public static final String SERVICES_THAT_CAN_INVOKE_CORE = "website-1";

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SpringBootSecurityUserDetailService springBootSecurityUserDetailService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        String authorisationHeader = request.getHeader(AUTHORIZATION);
        // Add this as part of Feign Interceptor
        // Should be removed from gateway if someone sends this
        String service = request.getHeader(X_SERVICE);

        if (StringUtils.hasText(authorisationHeader) &&
                authorisationHeader.startsWith("Bearer ") &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = authorisationHeader.substring(7);
            // This validated JWT as well
            String email = jwtService.getSubject(token);
            final UserContext userContext = jwtService.getUserContext(token);

            // User Repository call can be made as well if more information from user table is needed.
            // This call can be skipped as well
            UserDetails userDetails = springBootSecurityUserDetailService.loadUserByUsername(email);

            if (userDetails.getUsername().equals(email)) {
                // Once the JWT has been validated, create a new Authentication Object.
                // This Authentication Object has to be set in the Context.
                // The next Authentication filter uses this Authentication Object and understand that it does not have to authenticate
                // Similar to the check above "SecurityContextHolder.getContext().getAuthentication() == null"
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                User user = userRepository.findByEmail(email).get();
                request.setAttribute(USER_CONTEXT_ATTRIBUTE, userContext);
            }
        } else if (StringUtils.hasText(service) && service.equalsIgnoreCase(SERVICES_THAT_CAN_INVOKE_CORE)) {
            // Whenever a service calls this service, it will already have user context in headers
            final UserContext userContext = getUserContextFromHeaders(request);
            if (userContext != null) {
                UserDetails userDetails = springBootSecurityUserDetailService.loadUserByUserId(userContext.userId());

                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                    // It will be used in UserContextInterceptor
                    request.setAttribute(USER_CONTEXT_ATTRIBUTE, userContext);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserContext getUserContextFromHeaders(
            final HttpServletRequest request
    ) {
        String userId = request.getHeader("x-user-id");
        String tenantName = request.getHeader("x-tenant");
        String tenantId = request.getHeader("x-tenant-id");
        String tenantGuid = request.getHeader("x-tenant-guid");
        String permissionSetString = request.getHeader("x-permissions");

        if (!StringUtils.hasText(userId)) {
            return null;
        }

        UserContext.UserContextBuilder builder = UserContext.builder();
        builder.userId(UUID.fromString(userId));
        builder.tenant(tenantName);
        builder.tenantId(Long.parseLong(tenantId));
        builder.tenantGuid(UUID.fromString(tenantGuid));
        Set<String> permissions = Stream.of(
                        permissionSetString.substring(1, permissionSetString.length() - 1).split(",\\s*"))
                .collect(Collectors.toSet());
        builder.permissions(permissions);

        return builder.build();
    }

}
