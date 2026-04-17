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
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;
    
    private static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 1000 * 60 * 15; // 15 phút
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24 * 7; // 7 ngày
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    public String generateAccessToken(UserDetails userDetails, String fullName, String tenantId) {
        HashMap<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullName", fullName);
        extraClaims.put("tenantId", tenantId);
        extraClaims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        
        return buildToken(userDetails, extraClaims, ACCESS_TOKEN_EXPIRATION_MS);
    }

    public String generateRefreshToken(UserDetails userDetails, String tenantId) {
        HashMap<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("tenantId", tenantId);
        extraClaims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);

        return buildToken(userDetails, extraClaims, REFRESH_TOKEN_EXPIRATION_MS);
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class)));
    }

    /**
     * Thiết lập access token vào HttpOnly Cookie
     */
    public void setAccessCookie(HttpServletResponse response, String token) {
        String cookieValue = buildCookie(ACCESS_COOKIE_NAME, token, ACCESS_TOKEN_EXPIRATION_MS / 1000);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieValue);
    }

    /**
     * Thiết lập refresh token vào HttpOnly Cookie
     */
    public void setRefreshCookie(HttpServletResponse response, String token) {
        String cookieValue = buildCookie(REFRESH_COOKIE_NAME, token, REFRESH_TOKEN_EXPIRATION_MS / 1000);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieValue);
    }

    /**
     * Xóa cookie xác thực (dùng cho logout)
     */
    public void clearAllAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_COOKIE_NAME, "", 0));
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_COOKIE_NAME, "", 0));
        // Clear cookie cũ để tương thích ngược khi đổi cơ chế token
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("auth_token", "", 0));
    }

    public void clearAccessCookie(HttpServletResponse response) {
        String cookieValue = buildCookie(ACCESS_COOKIE_NAME, "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieValue);
    }

    /**
     * Lấy tên cookie access token
     */
    public String getAccessCookieName() {
        return ACCESS_COOKIE_NAME;
    }

    public String getRefreshCookieName() {
        return REFRESH_COOKIE_NAME;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private String buildToken(UserDetails userDetails, Map<String, Object> extraClaims, long ttlMs) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String buildCookie(String name, String value, long maxAgeSeconds) {
        String secureAttr = secureCookie ? "; Secure" : "";
        return String.format(
                "%s=%s; Path=/; HttpOnly%s; SameSite=Lax; Max-Age=%d",
                name,
                value,
                secureAttr,
                maxAgeSeconds
        );
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