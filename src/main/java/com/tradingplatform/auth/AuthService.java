package com.tradingplatform.auth;

import com.tradingplatform.common.ErrorMessages;
import com.tradingplatform.domain.User;
import com.tradingplatform.domain.enums.UserRole;
import com.tradingplatform.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(ErrorMessages.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(ErrorMessages.INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return new LoginResponse(
                token, user.getId(), user.getUsername(),
                user.getEmail(), user.getRole().name());
    }

    public LoginResponse register(String username, String email, String password, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AuthException(ErrorMessages.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new AuthException(ErrorMessages.USERNAME_ALREADY_TAKEN);
        }

        UserRole userRole;
        try {
            userRole = role != null ? UserRole.valueOf(role.toUpperCase()) : UserRole.ADMIN;
        } catch (IllegalArgumentException e) {
            userRole = UserRole.ADMIN;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(userRole);
        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(
                token, user.getId(), user.getUsername(),
                user.getEmail(), user.getRole().name());
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorMessages.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthException(ErrorMessages.WRONG_CURRENT_PASSWORD);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public record LoginResponse(
            String token, Long userId, String username, String email, String role) {}

    public static class AuthException extends RuntimeException {
        public AuthException(String msg) { super(msg); }
    }
}
