package com.smartmes.backend.modules.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')") // Chỉ Admin mới được vào đây
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<UserAccount> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserAccount user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        UserAccount user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok("Đã cập nhật mật khẩu mới!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa tài khoản!");
    }
}