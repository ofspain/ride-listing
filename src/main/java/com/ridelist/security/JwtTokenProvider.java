package com.ridelist.security;

import com.ridelist.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UserPrincipal userPrincipal) {
        return generateToken(userPrincipal.getId(), jwtProperties.getExpiration());
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        return generateToken(userPrincipal.getId(), jwtProperties.getRefreshExpiration());
    }

    private String generateToken(UUID userId, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessTokenExpiration() {
        return jwtProperties.getExpiration();
    }

    public static final long IMPERSONATION_TOKEN_EXPIRY = 30 * 60 * 1000L; // 30 minutes

    public String generateImpersonationToken(UUID userId, UUID adminId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + IMPERSONATION_TOKEN_EXPIRY);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("impersonatedBy", adminId.toString())
                .claim("isImpersonation", true)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isImpersonationToken(String token) {
        try {
            Claims claims = getClaims(token);
            Boolean isImpersonation = claims.get("isImpersonation", Boolean.class);
            return Boolean.TRUE.equals(isImpersonation);
        } catch (Exception ex) {
            return false;
        }
    }

    public String getImpersonatedBy(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("impersonatedBy", String.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
