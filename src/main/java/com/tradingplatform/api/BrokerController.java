package com.tradingplatform.api;

import com.tradingplatform.angelone.AngelOneAuthClient;
import com.tradingplatform.common.ErrorMessages;
import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.User;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/broker")
public class BrokerController {

    private final BrokerAccountRepository brokerAccountRepository;
    private final UserRepository userRepository;
    private final AngelOneAuthClient angelOneAuthClient;

    public BrokerController(BrokerAccountRepository brokerAccountRepository,
                            UserRepository userRepository,
                            AngelOneAuthClient angelOneAuthClient) {
        this.brokerAccountRepository = brokerAccountRepository;
        this.userRepository = userRepository;
        this.angelOneAuthClient = angelOneAuthClient;
    }

    @GetMapping("/my-account")
    public ResponseEntity<?> myAccount(Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            return brokerAccountRepository.findByUserIdAndBrokerName(userId, "ANGEL_ONE")
                    .map(a -> ResponseEntity.ok(Map.of(
                            "id", a.getId(),
                            "brokerName", a.getBrokerName(),
                            "clientCode", a.getClientCode(),
                            "isActive", a.isActive(),
                            "createdAt", a.getCreatedAt().toString()
                    )))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/connect/angel-one")
    public ResponseEntity<?> connectAngelOne(@RequestBody ConnectRequest req, Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            User user = userRepository.getReferenceById(userId);

            Optional<BrokerAccount> existing = brokerAccountRepository
                    .findByUserIdAndBrokerName(userId, "ANGEL_ONE");

            BrokerAccount account = existing.orElse(new BrokerAccount());
            account.setUser(user);
            account.setBrokerName("ANGEL_ONE");
            account.setClientCode(req.clientCode());
            account.setApiKey(req.apiKey());
            account.setPassword(req.password());
            account.setTotpSecret(req.totpSecret());
            account.setActive(true);
            if (account.getId() == null) account.setCreatedAt(Instant.now());
            account.setUpdatedAt(Instant.now());
            brokerAccountRepository.save(account);

            return ResponseEntity.ok(Map.of(
                    "message", "Angel One connected successfully",
                    "clientCode", req.clientCode()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            BrokerAccount account = brokerAccountRepository
                    .findByUserIdAndBrokerName(userId, "ANGEL_ONE")
                    .orElseThrow(() -> new RuntimeException("No Angel One account connected"));
            angelOneAuthClient.loginForAccount(account.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "Connection successful!",
                    "clientCode", account.getClientCode()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            brokerAccountRepository.findByUserIdAndBrokerName(userId, "ANGEL_ONE")
                    .ifPresent(brokerAccountRepository::delete);
            return ResponseEntity.ok(Map.of("message", "Broker disconnected"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ErrorMessages.INTERNAL_ERROR));
        }
    }

    // ── helpers ──

    private Long getUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email))
                .getId();
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ErrorMessages.NOT_AUTHENTICATED));
    }

    public record ConnectRequest(
            @NotBlank String clientCode,
            @NotBlank String apiKey,
            @NotBlank String password,
            @NotBlank String totpSecret
    ) {}
}