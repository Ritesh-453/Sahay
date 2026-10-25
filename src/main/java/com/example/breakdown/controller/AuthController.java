package com.example.breakdown.controller;

import com.example.breakdown.config.JwtUtil;
import com.example.breakdown.model.User;
import com.example.breakdown.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:8082"
})
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String password = data.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank())
            return ResponseEntity.badRequest().body("Username and password are required.");

        if (userRepository.findByUsername(username).isPresent())
            return ResponseEntity.badRequest().body("Username already exists!");

        userRepository.save(new User(null, username, passwordEncoder.encode(password), "CUSTOMER"));
        return ResponseEntity.ok("Registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        String password = data.get("password");

        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                    return ResponseEntity.ok(Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "role", user.getRole()
                    ));
                })
                .orElse(ResponseEntity.status(401).build());
    }
}