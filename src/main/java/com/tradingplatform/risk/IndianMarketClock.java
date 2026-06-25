package com.tradingplatform.risk;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * "What trading day is it?" must always be answered in IST (NSE/BSE's timezone),
 * regardless of where the server actually runs. Relying on the server's default
 * timezone would silently roll daily_pnl over at the wrong moment if this is ever
 * deployed somewhere other than an IST host.
 */
public final class IndianMarketClock {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private IndianMarketClock() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static Clock clock() {
        return Clock.system(ZONE);
    }
}
