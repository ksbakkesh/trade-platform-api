package com.tradingplatform.api;

import com.tradingplatform.domain.User;
import com.tradingplatform.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Admin endpoint to list all platform users.
 * Used by the User Management page in the frontend.
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserRepository userRepository;

    public UserManagementController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public record UserResponse(
            Long id,
            String username,
            String email,
            String role,
            boolean active,
            Instant createdAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getRole().name(),
                    u.isActive(),
                    u.getCreatedAt()
            );
        }
    }
}
