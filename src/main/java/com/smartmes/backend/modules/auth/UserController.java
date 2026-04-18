package com.smartmes.backend.modules.auth;

import com.smartmes.backend.core.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserAccount account)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        return ResponseEntity.ok(new MeResponse(
                account.getUsername(),
                account.getFullName(),
                account.getRole(),
                account.getTenantId()
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findByTenantIdAndActiveTrueOrderByIdDesc(SecurityUtils.getCurrentTenantId()).stream()
                .map(UserResponse::new)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody UserAccount user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại!");
        }
        user.setTenantId(SecurityUtils.getCurrentTenantId());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        UserAccount saved = userRepository.save(user);
        return ResponseEntity.ok(new UserResponse(saved));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("Mật khẩu mới không được để trống!");
        }
        
        UserAccount user = userRepository.findByIdAndTenantId(id, SecurityUtils.getCurrentTenantId()).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok("Đã cập nhật mật khẩu mới!");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        UserAccount user = userRepository.findByIdAndTenantId(id, SecurityUtils.getCurrentTenantId()).orElseThrow();
        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok("Đã xóa tài khoản!");
    }
}

record MeResponse(String username, String fullName, String role, String tenantId) {}

record UserResponse(Long id, String username, String fullName, String role, boolean active, String tenantId) {
    UserResponse(UserAccount account) {
        this(account.getId(), account.getUsername(), account.getFullName(), account.getRole(), account.isActive(), account.getTenantId());
    }
}