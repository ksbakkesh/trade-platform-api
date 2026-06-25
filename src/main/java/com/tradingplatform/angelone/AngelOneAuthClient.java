package com.tradingplatform.angelone;

import com.tradingplatform.angelone.dto.AngelOneApiResponse;
import com.tradingplatform.angelone.dto.LoginRequest;
import com.tradingplatform.angelone.dto.LoginResponseData;
import com.tradingplatform.angelone.dto.RefreshTokenRequest;
import com.tradingplatform.config.AngelOneProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Handles the Angel One SmartAPI authentication lifecycle:
 *   1. login()        - clientcode + password + TOTP -> jwt/refresh/feed tokens
 *   2. ensureLoggedIn() - call this before any other API call; logs in if needed
 *   3. refresh()       - rotate the jwt using the refresh token (cheaper than a full login)
 *   4. logout()        - invalidate the session on Angel One's side
 *
 * Downstream clients (AngelOneMarketClient, AngelOneOrderClient) should call
 * ensureLoggedIn() then tokenStore.getJwtToken() to build their Authorization header.
 */
@Component
public class AngelOneAuthClient {

    private static final Logger log = LoggerFactory.getLogger(AngelOneAuthClient.class);

    private static final String LOGIN_PATH = "/rest/auth/angelbroking/user/v1/loginByPassword";
    private static final String REFRESH_PATH = "/rest/auth/angelbroking/jwt/v1/generateTokens";
    private static final String LOGOUT_PATH = "/rest/secure/angelbroking/user/v1/logout";

    private final RestClient restClient;
    private final AngelOneProperties props;
    private final TotpGenerator totpGenerator;
    private final AngelOneTokenStore tokenStore;

    public AngelOneAuthClient(RestClient angelOneRestClient,
                               AngelOneProperties props,
                               TotpGenerator totpGenerator,
                               AngelOneTokenStore tokenStore) {
        this.restClient = angelOneRestClient;
        this.props = props;
        this.totpGenerator = totpGenerator;
        this.tokenStore = tokenStore;
    }

    /** Ensures there's a valid session, logging in fresh if there isn't one yet. */
    public synchronized void ensureLoggedIn() {
        if (!tokenStore.hasValidSession()) {
            login();
        }
    }

    public synchronized void login() {
        String totp = totpGenerator.generate(props.getTotpSecret());
        LoginRequest body = new LoginRequest(props.getClientCode(), props.getPassword(), totp);

        log.info("Logging in to Angel One SmartAPI for client {}", props.getClientCode());

        AngelOneApiResponse<LoginResponseData> response = restClient.post()
                .uri(LOGIN_PATH)
                .body(body)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<AngelOneApiResponse<LoginResponseData>>() {});

        requireSuccess(response, "Login failed");

        LoginResponseData data = response.getData();
        tokenStore.save(data.getJwtToken(), data.getRefreshToken(), data.getFeedToken());
        log.info("Angel One login successful for client {}", props.getClientCode());
    }

    /** Rotates the JWT using the stored refresh token. Cheaper than a full re-login. */
    public synchronized void refresh() {
        RefreshTokenRequest body = new RefreshTokenRequest(tokenStore.getRefreshToken());

        AngelOneApiResponse<LoginResponseData> response = restClient.post()
                .uri(REFRESH_PATH)
                .body(body)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<AngelOneApiResponse<LoginResponseData>>() {});

        requireSuccess(response, "Token refresh failed");

        LoginResponseData data = response.getData();
        // generateTokens only returns a new jwtToken; keep the existing refresh/feed tokens.
        tokenStore.save(data.getJwtToken(), tokenStore.getRefreshToken(), tokenStore.getFeedToken());
        log.info("Angel One token refreshed for client {}", props.getClientCode());
    }

    public synchronized void logout() {
        if (!tokenStore.hasValidSession()) {
            return;
        }
        try {
            restClient.post()
                    .uri(LOGOUT_PATH)
                    .header("Authorization", "Bearer " + tokenStore.getJwtToken())
                    .body(new ClientCodeBody(props.getClientCode()))
                    .retrieve()
                    .toBodilessEntity();
        } finally {
            tokenStore.clear();
        }
    }

    private void requireSuccess(AngelOneApiResponse<?> response, String contextMessage) {
        if (response == null || !response.isStatus() || response.getData() == null) {
            String message = response != null ? response.getMessage() : "null response";
            String errorCode = response != null ? response.getErrorcode() : "UNKNOWN";
            throw new AngelOneApiException(contextMessage + ": " + message, errorCode);
        }
    }

    private record ClientCodeBody(String clientcode) {
    }
}
