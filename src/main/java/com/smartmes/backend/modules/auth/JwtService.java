package com.smartmes.backend.modules.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secretKey;
    
    private static final String AUTH_COOKIE_NAME = "auth_token";
    private static final long TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24h

    public String generateToken(UserDetails userDetails, String fullName, String tenantId) {
        HashMap<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullName", fullName);
        extraClaims.put("tenantId", tenantId);
        
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_MS))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Thiết lập JWT vào HttpOnly Cookie
     */
    public void setAuthCookie(HttpServletResponse response, String token) {
        String cookieValue = String.format(
                "%s=%s; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=%d",
                AUTH_COOKIE_NAME,
                token,
                TOKEN_EXPIRATION_MS / 1000 // Chuyển sang giây
        );
        response.addHeader(HttpHeaders.SET_COOKIE, cookieValue);
    }

    /**
     * Xóa cookie xác thực (dùng cho logout)
     */
    public void clearAuthCookie(HttpServletResponse response) {
        String cookieValue = String.format("%s=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0", AUTH_COOKIE_NAME);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieValue);
    }

    /**
     * Lấy tên của cookie xác thực
     */
    public String getAuthCookieName() {
        return AUTH_COOKIE_NAME;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
        return claimsResolver.apply(claims);
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}