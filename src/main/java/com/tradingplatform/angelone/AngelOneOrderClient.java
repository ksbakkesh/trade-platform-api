package com.tradingplatform.angelone;

import com.tradingplatform.angelone.dto.AngelOneApiResponse;
import com.tradingplatform.angelone.dto.OrderResponse;
import com.tradingplatform.angelone.dto.PlaceOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Places orders through Angel One SmartAPI.
 *
 * NOTE: this client deliberately does NOT contain any strategy, risk, or
 * position-sizing logic - it only knows how to send a well-formed order and
 * report what came back. The risk management module (max trades/day, daily
 * loss limit, margin validation) sits in front of this and decides whether
 * a call to placeOrder() should happen at all.
 */
@Component
public class AngelOneOrderClient {

    private static final Logger log = LoggerFactory.getLogger(AngelOneOrderClient.class);
    private static final String PLACE_ORDER_PATH = "/rest/secure/angelbroking/order/v1/placeOrder";

    private final RestClient restClient;
    private final AngelOneAuthClient authClient;
    private final AngelOneTokenStore tokenStore;

    public AngelOneOrderClient(RestClient angelOneRestClient,
                                AngelOneAuthClient authClient,
                                AngelOneTokenStore tokenStore) {
        this.restClient = angelOneRestClient;
        this.authClient = authClient;
        this.tokenStore = tokenStore;
    }

    public OrderResponse placeOrder(PlaceOrderRequest order) {
        authClient.ensureLoggedIn();

        log.info("Placing order: {} {} x{} ({})",
                order.getTransactiontype(), order.getTradingsymbol(), order.getQuantity(), order.getOrdertype());

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri(PLACE_ORDER_PATH)
                    .header("Authorization", "Bearer " + tokenStore.getJwtToken())
                    .body(order)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new AngelOneApiException("placeOrder HTTP error: " + e.getMessage(), e);
        }

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AngelOneApiException("placeOrder returned empty response", "EMPTY_RESPONSE");
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            AngelOneApiResponse<OrderResponse> response = mapper.readValue(rawResponse,
                    mapper.getTypeFactory().constructParametricType(
                            AngelOneApiResponse.class, OrderResponse.class));

            if (response == null || !response.isStatus()) {
                String message = response != null ? response.getMessage() : "null response";
                String errorCode = response != null ? response.getErrorcode() : "UNKNOWN";
                log.error("Order placement failed: {} ({}) | raw: {}", message, errorCode, rawResponse);
                throw new AngelOneApiException("placeOrder failed: " + message, errorCode);
            }

            log.info("Order placed successfully: orderId={}", response.getData().getOrderid());
            return response.getData();

        } catch (AngelOneApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse order response: {} | raw response: {}", e.getMessage(), rawResponse);
            throw new AngelOneApiException("placeOrder response parse error: " + e.getMessage()
                    + " | raw: " + rawResponse, "PARSE_ERROR");
        }
    }
}
