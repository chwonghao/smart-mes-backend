package com.smartmes.backend.modules.auth;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;

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

    // TỰ ĐỘNG TẠO TÀI KHOẢN ADMIN KHI CHẠY SERVER LẦN ĐẦU
    @PostConstruct
    public void initAdminAccount() {
        if (userRepository.count() == 0) {
            UserAccount admin = new UserAccount();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("Giám đốc Hệ thống");
            admin.setRole("ROLE_ADMIN");
            admin.setTenantId("TENANT_01");
            userRepository.save(admin);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        var user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        var token = jwtService.generateToken(user, user.getFullName(), user.getTenantId());
        
        return ResponseEntity.ok(new LoginResponse(token, user.getFullName(), user.getRole()));
    }
}

@Data class LoginRequest { private String username; private String password; }
@Data class LoginResponse { 
    private String token; private String fullName; private String role;
    public LoginResponse(String token, String fullName, String role) { this.token = token; this.fullName = fullName; this.role = role;} 
}