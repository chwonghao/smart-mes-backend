package com.smartmes.backend.modules.auth;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-tenant:TENANT_DEFAULT}")
    private String defaultTenantId;

    // TỰ ĐỘNG TẠO TÀI KHOẢN ADMIN KHI CHẠY SERVER LẦN ĐẦU
    @PostConstruct
    public void initAdminAccount() {
        if (userRepository.count() == 0) {
            UserAccount admin = new UserAccount();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("Giám đốc Hệ thống");
            admin.setRole("ROLE_ADMIN");
            admin.setTenantId(defaultTenantId);
            userRepository.save(admin);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        var user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        var accessToken = jwtService.generateAccessToken(user, user.getFullName(), user.getTenantId());
        var refreshToken = jwtService.generateRefreshToken(user, user.getTenantId());
        
        // Thiết lập access + refresh token vào HttpOnly Cookie
        jwtService.setAccessCookie(response, accessToken);
        jwtService.setRefreshCookie(response, refreshToken);
        
        return ResponseEntity.ok(new LoginResponse(user.getFullName(), user.getRole()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, jwtService.getRefreshCookieName());
        if (refreshToken == null || refreshToken.isBlank()) {
            jwtService.clearAllAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing refresh token");
        }

        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                jwtService.clearAllAuthCookies(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token type");
            }

            String username = jwtService.extractUsername(refreshToken);
            String tenantId = jwtService.extractTenantId(refreshToken);
            UserAccount account = userRepository.findByUsername(username).orElse(null);

            if (account == null || account.getTenantId() == null || !account.getTenantId().equals(tenantId)) {
                jwtService.clearAllAuthCookies(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token context");
            }

            UserDetails userDetails = account;
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                jwtService.clearAllAuthCookies(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
            }

            String newAccessToken = jwtService.generateAccessToken(account, account.getFullName(), account.getTenantId());
            String newRefreshToken = jwtService.generateRefreshToken(account, account.getTenantId());

            // Rotate cả 2 token để giảm nguy cơ replay token
            jwtService.setAccessCookie(response, newAccessToken);
            jwtService.setRefreshCookie(response, newRefreshToken);

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            jwtService.clearAllAuthCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Xóa toàn bộ cookie xác thực
        jwtService.clearAllAuthCookies(response);
        return ResponseEntity.ok("Logged out successfully");
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}

@Data class LoginRequest { 
    private String username; 
    private String password; 
}

@Data class LoginResponse { 
    private String fullName; 
    private String role;
    
    public LoginResponse(String fullName, String role) { 
        this.fullName = fullName; 
        this.role = role;
    } 
}