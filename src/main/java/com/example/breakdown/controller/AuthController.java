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

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> data) {
    String username = data.get("username");
    String password = data.get("password");

    System.out.println("Username: " + username);
    System.out.println("User found: " + userRepository.findByUsername(username).isPresent());

    userRepository.findByUsername(username).ifPresent(user -> {
        System.out.println("Stored hash: " + user.getPassword());
        System.out.println("Password matches: " +
                passwordEncoder.matches(password, user.getPassword()));
    });

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