package com.tradingplatform.angelone;

import com.tradingplatform.angelone.dto.AngelOneApiResponse;
import com.tradingplatform.angelone.dto.ProfileResponse;
import com.tradingplatform.angelone.dto.QuoteRequest;
import com.tradingplatform.angelone.dto.QuoteResponse;
import com.tradingplatform.angelone.dto.FundsResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Read-only market data calls: account profile and live quotes (LTP/OHLC).
 * Every call here ensures a valid session first, so callers never have to
 * think about login/refresh themselves.
 */
@Component
public class AngelOneMarketClient {

    private static final String PROFILE_PATH = "/rest/secure/angelbroking/user/v1/getProfile";
    private static final String QUOTE_PATH = "/rest/secure/angelbroking/market/v1/quote/";
    private static final String FUNDS_PATH = "/rest/secure/angelbroking/user/v1/getRMS";

    private final RestClient restClient;
    private final AngelOneAuthClient authClient;
    private final AngelOneTokenStore tokenStore;

    public AngelOneMarketClient(RestClient angelOneRestClient,
                                 AngelOneAuthClient authClient,
                                 AngelOneTokenStore tokenStore) {
        this.restClient = angelOneRestClient;
        this.authClient = authClient;
        this.tokenStore = tokenStore;
    }

    public ProfileResponse getProfile() {
        authClient.ensureLoggedIn();

        AngelOneApiResponse<ProfileResponse> response = restClient.get()
                .uri(PROFILE_PATH)
                .header("Authorization", "Bearer " + tokenStore.getJwtToken())
                .retrieve()
                .body(new ParameterizedTypeReference<AngelOneApiResponse<ProfileResponse>>() {});

        requireSuccess(response, "getProfile failed");
        return response.getData();
    }

    /**
     * Fetches live quote data (LTP, OHLC, volume) for one or more symbol tokens on an exchange.
     *
     * @param exchange      e.g. "NSE", "NFO", "BSE"
     * @param symbolTokens  the broker's numeric tokens for each instrument (not trading symbols)
     * @param mode          "LTP", "OHLC", or "FULL"
     */
    public QuoteResponse getQuote(String exchange, List<String> symbolTokens, String mode) {
        authClient.ensureLoggedIn();

        QuoteRequest body = new QuoteRequest(mode, Map.of(exchange, symbolTokens));

        AngelOneApiResponse<QuoteResponse> response = restClient.post()
                .uri(QUOTE_PATH)
                .header("Authorization", "Bearer " + tokenStore.getJwtToken())
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<AngelOneApiResponse<QuoteResponse>>() {});

        requireSuccess(response, "getQuote failed");
        return response.getData();
    }

    /**
     * Fetches current account funds and margin from Angel One's RMS endpoint.
     * Called before every order for BRD Section 11 fund validation.
     */
    public FundsResponse getFunds() {
        authClient.ensureLoggedIn();

        AngelOneApiResponse<FundsResponse> response = restClient.get()
                .uri(FUNDS_PATH)
                .header("Authorization", "Bearer " + tokenStore.getJwtToken())
                .retrieve()
                .body(new ParameterizedTypeReference<AngelOneApiResponse<FundsResponse>>() {});

        requireSuccess(response, "getFunds failed");
        return response.getData();
    }

    private void requireSuccess(AngelOneApiResponse<?> response, String contextMessage) {
        if (response == null || !response.isStatus()) {
            String message = response != null ? response.getMessage() : "null response";
            String errorCode = response != null ? response.getErrorcode() : "UNKNOWN";
            throw new AngelOneApiException(contextMessage + ": " + message, errorCode);
        }
    }
}
