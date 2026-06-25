package com.tradingplatform.angelone;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the current Angel One session tokens in memory.
 *
 * This is intentionally simple for now (single broker account, single JVM instance).
 * When we add multi-account support / horizontal scaling, swap this for a Redis-backed
 * store (Redis is already in the BRD's tech stack) keyed by broker_account id.
 */
@Component
public class AngelOneTokenStore {

    /** Angel One JWTs are valid for a trading session; we treat them as expired after this. */
    private static final long JWT_TTL_SECONDS = 6 * 60 * 60; // 6 hours, conservative

    private final AtomicReference<Session> currentSession = new AtomicReference<>();

    public void save(String jwtToken, String refreshToken, String feedToken) {
        currentSession.set(new Session(jwtToken, refreshToken, feedToken, Instant.now()));
    }

    public void clear() {
        currentSession.set(null);
    }

    public boolean hasValidSession() {
        Session session = currentSession.get();
        if (session == null) {
            return false;
        }
        return Instant.now().isBefore(session.issuedAt.plusSeconds(JWT_TTL_SECONDS));
    }

    public String getJwtToken() {
        return requireSession().jwtToken;
    }

    public String getRefreshToken() {
        return requireSession().refreshToken;
    }

    public String getFeedToken() {
        return requireSession().feedToken;
    }

    private Session requireSession() {
        Session session = currentSession.get();
        if (session == null) {
            throw new IllegalStateException("No Angel One session found - call login() first");
        }
        return session;
    }

    private record Session(String jwtToken, String refreshToken, String feedToken, Instant issuedAt) {
    }
}
