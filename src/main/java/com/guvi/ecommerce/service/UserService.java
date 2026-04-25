package com.guvi.ecommerce.service;

import com.guvi.ecommerce.dto.AuthResponse;
import com.guvi.ecommerce.dto.LoginRequest;
import com.guvi.ecommerce.dto.RegisterRequest;
import com.guvi.ecommerce.model.User;
import com.guvi.ecommerce.repo.UserRepository;
import com.guvi.ecommerce.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                List.of("ROLE_USER")
        );
        userRepository.save(user);
        log.info("New user registered: {}", request.getEmail());
        String token = jwtUtil.generateToken(user.getEmail(), "ROLE_USER");
        return new AuthResponse(token, user.getEmail(), "ROLE_USER");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        String role = user.getRoles().get(0);
        String token = jwtUtil.generateToken(user.getEmail(), role);
        log.info("User logged in: {}", request.getEmail());
        return new AuthResponse(token, user.getEmail(), role);
    }
}
