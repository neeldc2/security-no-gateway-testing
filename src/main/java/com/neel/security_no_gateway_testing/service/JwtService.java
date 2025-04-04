package com.neel.security_no_gateway_testing.service;

import com.neel.security_no_gateway_testing.dto.ValidateResponse;
import com.neel.security_no_gateway_testing.exception.WebsiteException;
import com.neel.security_no_gateway_testing.usercontext.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class JwtService {

    // 10 mins
    //@Value("${website-login.access-token-expiration-in-millis:600000}")
    @Value("${website-login.access-token-expiration-in-millis:20000}")
    private Long accessTokenExpirationInMillis;

    // 2 Hours
    @Value("${website-login.refresh-token-expiration-in-millis:7200000}")
    private Long refreshTokenExpirationInMillis;

    // 3 days
    @Value("${website-login.add-tenant-refresh-token-expiration-in-millis:259200000}")
    private Long addTenantRefreshTokenExpirationInMillis;

    private final String secretKey;

    public JwtService() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey key = keyGenerator.generateKey();
            secretKey = Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new WebsiteException(e);
        }
    }

    public String generateJwtToken(final String username,
                                   final Map<String, Object> claims) {
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (accessTokenExpirationInMillis)))
                .and()
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(final String email,
                                       final Long tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (refreshTokenExpirationInMillis)))
                .and()
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(final String email,
                                       final Long tenantId,
                                       final Long timeInMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId);
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (timeInMillis)))
                .and()
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshTokenForNewTenant(final String email,
                                                   final Long tenantId) {
        return generateRefreshToken(email, tenantId, addTenantRefreshTokenExpirationInMillis);
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String getSubject(final String token) {
        return getAllClaims(token).getSubject();
    }

    public UserContext getUserContext(final String token) {
        final Claims claims = getAllClaims(token);

        String permissionSetString = claims.get("permissions").toString();
        Set<String> permissions = Stream.of(
                        permissionSetString.substring(1, permissionSetString.length() - 1).split(",\\s*"))
                .collect(Collectors.toSet());

        return UserContext.builder()
                .userId(UUID.fromString(claims.get("userId").toString()))
                .tenant(claims.get("tenant").toString())
                .tenantId(Long.parseLong(claims.get("tenantId").toString()))
                .tenantGuid(UUID.fromString(claims.get("tenantGuid").toString()))
                .permissions(permissions)
                .build();
    }

    public ValidateResponse getValidateResponse(final String token) {
        final Claims claims = getAllClaims(token);

        String permissionSetString = claims.get("permissions").toString();
        Set<String> permissions = Stream.of(
                        permissionSetString.substring(1, permissionSetString.length() - 1).split(",\\s*"))
                .collect(Collectors.toSet());

        final UserContext userContext = UserContext.builder()
                .userId(UUID.fromString(claims.get("userId").toString()))
                .tenant(claims.get("tenant").toString())
                .tenantId(Long.parseLong(claims.get("tenantId").toString()))
                .tenantGuid(UUID.fromString(claims.get("tenantGuid").toString()))
                .permissions(permissions)
                .build();

        return ValidateResponse.builder()
                .userContext(userContext)
                .expiryTime(Long.parseLong(claims.get("exp").toString()))
                .build();
    }

    public Long getTenantId(final String token) {
        final Claims claims = getAllClaims(token);
        return Long.parseLong(claims.get("tenantId").toString());
    }

    /**
     * This method need not be called.
     * Expiration is verified when getAllClaims() is called.
     *
     * @param token
     * @param userDetails
     * @return
     */
    public boolean validateToken(final String token, final UserDetails userDetails) {
        String email = getSubject(token);

        if (userDetails.getUsername().equals(email)) {
            Date expirationDate = getClaim(token, Claims::getExpiration);
            return expirationDate.after(new Date());
        }

        return false;
    }

    private Claims getAllClaims(final String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    private <T> T getClaim(final String token, Function<Claims, T> claimsFunction) {
        return claimsFunction.apply(getAllClaims(token));
    }

}
