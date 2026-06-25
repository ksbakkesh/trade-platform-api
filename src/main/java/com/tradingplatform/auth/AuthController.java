package com.tradingplatform.auth;

import com.tradingplatform.common.ErrorMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints.
 *
 * POST /api/auth/login    — email + password → JWT token
 * POST /api/auth/register — create new user (admin only in production)
 * GET  /api/auth/me       — returns current user info from token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            AuthService.LoginResponse response = authService.login(req.email(), req.password());
            return ResponseEntity.ok(response);
        } catch (AuthService.AuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ErrorMessages.INTERNAL_ERROR));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            AuthService.LoginResponse response = authService.register(
                    req.username(), req.email(), req.password(), req.role());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AuthService.AuthException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            String msg = e.getMessage() != null && e.getMessage().contains("username")
                    ? ErrorMessages.USERNAME_ALREADY_TAKEN
                    : ErrorMessages.EMAIL_ALREADY_EXISTS;
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ErrorMessages.INTERNAL_ERROR));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ErrorMessages.NOT_AUTHENTICATED));
        }
        return ResponseEntity.ok(Map.of(
                "email", auth.getName(),
                "userId", auth.getDetails(),
                "role", auth.getAuthorities().iterator().next().getAuthority()
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req,
                                             Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ErrorMessages.NOT_AUTHENTICATED));
        }
        try {
            Long userId = (Long) auth.getDetails();
            authService.changePassword(userId, req.currentPassword(), req.newPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (AuthService.AuthException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ErrorMessages.INTERNAL_ERROR));
        }
    }

    // ---- Request DTOs ----

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            String role
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6) String newPassword
    ) {}
}
