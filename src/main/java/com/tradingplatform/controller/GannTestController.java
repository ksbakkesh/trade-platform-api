package com.tradingplatform.controller;

import com.tradingplatform.angelone.AngelOneMarketClient;
import com.tradingplatform.angelone.AngelOneApiException;
import com.tradingplatform.angelone.dto.QuoteResponse;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.strategy.gann.GannCalculationService;
import com.tradingplatform.strategy.gann.GannLevels;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manual verification endpoints for the Gann calculation engine.
 *
 * Example (manual open price):
 *   curl "http://localhost:8080/api/test/gann/levels?index=NIFTY&openPrice=24350.50"
 *
 * Example (live open price via Angel One - Section 3's "Automatic Mode"):
 *   curl "http://localhost:8080/api/test/gann/levels/live?index=NIFTY&exchange=NSE&token=99926000"
 *
 * NOTE: SENSEX's NSE/BSE symbol token isn't hardcoded here since getting it wrong
 * would silently price the wrong instrument - look it up from Angel One's scrip
 * master (https://margincalculator.angelone.in/OpenAPI_File/files/OpenAPIScripMaster.json)
 * before testing SENSEX live.
 */
@RestController
public class GannTestController {

    private final GannCalculationService gannCalculationService;
    private final AngelOneMarketClient angelOneMarketClient;

    public GannTestController(GannCalculationService gannCalculationService,
                               AngelOneMarketClient angelOneMarketClient) {
        this.gannCalculationService = gannCalculationService;
        this.angelOneMarketClient = angelOneMarketClient;
    }

    @GetMapping("/api/test/gann/levels")
    public GannLevels levels(@RequestParam IndexName index, @RequestParam BigDecimal openPrice) {
        return gannCalculationService.calculate(index, openPrice);
    }

    @GetMapping("/api/test/gann/levels/live")
    public GannLevels levelsFromLiveOpenPrice(@RequestParam IndexName index,
                                               @RequestParam String exchange,
                                               @RequestParam String token) {
        QuoteResponse quote = angelOneMarketClient.getQuote(exchange, List.of(token), "FULL");

        if (quote.getFetched() == null || quote.getFetched().isEmpty()) {
            throw new AngelOneApiException("No quote data returned for token " + token, "NO_DATA");
        }

        Double openPrice = quote.getFetched().get(0).getOpen();
        if (openPrice == null) {
            throw new AngelOneApiException(
                    "Open price not available yet for token " + token + " (market may not be open)", "NO_OPEN_PRICE");
        }

        return gannCalculationService.calculate(index, BigDecimal.valueOf(openPrice));
    }
}

