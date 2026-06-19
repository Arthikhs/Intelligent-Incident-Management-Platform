package com.iimp.user.service;

import com.iimp.common.exception.IIMPException;
import com.iimp.common.security.JwtTokenProvider;
import com.iimp.user.domain.User;
import com.iimp.user.dto.AuthRequest;
import com.iimp.user.dto.AuthResponse;
import com.iimp.user.dto.RegisterRequest;
import com.iimp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw IIMPException.conflict("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw IIMPException.conflict("Username already in use: " + request.getUsername());
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole() != null ? request.getRole() : User.Role.VIEWER)
            .active(true)
            .build();

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId().toString(), user.getUsername(), user.getRole().name());

        log.info("User registered: username={}, role={}", user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getId().toString(), user.getUsername(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> IIMPException.unauthorized("Invalid credentials"));

        if (!user.isActive()) throw IIMPException.unauthorized("Account is deactivated");
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw IIMPException.unauthorized("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(user.getId().toString(), user.getUsername(), user.getRole().name());
        log.info("User logged in: username={}", user.getUsername());
        return new AuthResponse(token, user.getId().toString(), user.getUsername(), user.getRole().name());
    }
}
