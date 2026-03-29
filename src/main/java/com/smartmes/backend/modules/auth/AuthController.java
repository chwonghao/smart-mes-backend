package com.smartmes.backend.modules.auth;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        var token = jwtService.generateToken(user, user.getFullName(), user.getTenantId());
        
        // Thiết lập JWT vào HttpOnly Cookie
        jwtService.setAuthCookie(response, token);
        
        return ResponseEntity.ok(new LoginResponse(user.getFullName(), user.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Xóa cookie xác thực
        jwtService.clearAuthCookie(response);
        return ResponseEntity.ok("Logged out successfully");
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