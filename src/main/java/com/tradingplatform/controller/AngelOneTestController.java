package com.tradingplatform.controller;

import com.tradingplatform.angelone.AngelOneAuthClient;
import com.tradingplatform.angelone.AngelOneMarketClient;
import com.tradingplatform.angelone.dto.ProfileResponse;
import com.tradingplatform.angelone.dto.QuoteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Manual verification endpoints for the Angel One integration.
 * Not meant to be the final API surface - just hit these with curl/Postman
 * to confirm login, profile, and quote fetching actually work end to end
 * before we build the strategy engine on top of it.
 *
 * Example:
 *   curl -X POST http://localhost:8080/api/test/angelone/login
 *   curl http://localhost:8080/api/test/angelone/profile
 *   curl "http://localhost:8080/api/test/angelone/quote?exchange=NSE&token=99926000&mode=LTP"
 *   (99926000 is NIFTY 50's NSE token, for quick sanity testing)
 */
@RestController
public class AngelOneTestController {

    private final AngelOneAuthClient authClient;
    private final AngelOneMarketClient marketClient;

    public AngelOneTestController(AngelOneAuthClient authClient, AngelOneMarketClient marketClient) {
        this.authClient = authClient;
        this.marketClient = marketClient;
    }

    @PostMapping("/api/test/angelone/login")
    public Map<String, String> login() {
        authClient.login();
        return Map.of("status", "logged in");
    }

    @PostMapping("/api/test/angelone/logout")
    public Map<String, String> logout() {
        authClient.logout();
        return Map.of("status", "logged out");
    }

    @GetMapping("/api/test/angelone/profile")
    public ProfileResponse profile() {
        return marketClient.getProfile();
    }

    @GetMapping("/api/test/angelone/quote")
    public QuoteResponse quote(@RequestParam String exchange,
                                @RequestParam String token,
                                @RequestParam(defaultValue = "LTP") String mode) {
        return marketClient.getQuote(exchange, List.of(token), mode);
    }

    @GetMapping("/api/test/angelone/funds")
    public Object funds() {
        return marketClient.getFunds();
    }
}
