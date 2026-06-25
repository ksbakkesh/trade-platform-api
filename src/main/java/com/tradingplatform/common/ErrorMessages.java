package com.tradingplatform.common;

/**
 * Centralised error message constants.
 * All user-facing error strings live here — no hardcoded strings scattered across services.
 * Update once here, takes effect everywhere.
 */
public final class ErrorMessages {

    private ErrorMessages() {} // utility class — no instances

    // ── Auth ──────────────────────────────────────────────────────────────────
    public static final String INVALID_CREDENTIALS     = "Invalid email or password. Please try again.";
    public static final String EMAIL_ALREADY_EXISTS    = "This email is already registered. Please use a different email.";
    public static final String USERNAME_ALREADY_TAKEN  = "This username is already taken. Please choose a different name.";
    public static final String USER_NOT_FOUND          = "User not found.";
    public static final String WRONG_CURRENT_PASSWORD  = "Current password is incorrect.";
    public static final String PASSWORD_TOO_SHORT      = "Password must be at least 6 characters.";
    public static final String NOT_AUTHENTICATED       = "You are not logged in. Please sign in to continue.";
    public static final String REGISTER_FAILED         = "Failed to create account. Please try again.";

    // ── Risk ──────────────────────────────────────────────────────────────────
    public static final String MAX_TRADES_REACHED      = "Maximum trades per day reached. Trading blocked for today.";
    public static final String DAILY_LOSS_LIMIT_HIT    = "Daily loss limit reached. Trading blocked for today.";
    public static final String TRADING_DISABLED        = "Trading is manually disabled for today.";
    public static final String AUTO_TRADING_OFF        = "Auto-trading is disabled in strategy settings.";

    // ── Signal ────────────────────────────────────────────────────────────────
    public static final String NO_SIGNAL_DIRECTION     = "Spot price is between Buy Above and Sell Below — no directional bias.";
    public static final String SIGNAL_NOT_GENERATED    = "Signal was not generated. Check entry conditions.";
    public static final String NO_STRATEGY_SETTINGS    = "No strategy settings found for this account and index.";

    // ── Position / Trade ──────────────────────────────────────────────────────
    public static final String INSUFFICIENT_CAPITAL    = "Insufficient capital to size even 1 lot at this premium.";
    public static final String INSUFFICIENT_MARGIN     = "Insufficient margin available in your Angel One account.";
    public static final String TRADE_NOT_FOUND         = "Trade not found.";
    public static final String SIGNAL_NOT_FOUND        = "Signal not found.";
    public static final String POSITION_NOT_FOUND      = "Position not found for this trade.";
    public static final String SIGNAL_NOT_TRADEABLE    = "Only GENERATED signals can be executed.";

    // ── Broker ────────────────────────────────────────────────────────────────
    public static final String BROKER_ORDER_FAILED     = "Broker rejected the order. Please check the symbol and try again.";
    public static final String BROKER_SESSION_EXPIRED  = "Angel One session expired. Please refresh to re-login.";
    public static final String BROKER_IP_NOT_REGISTERED = "Your IP address is not registered with Angel One SmartAPI.";

    // ── General ───────────────────────────────────────────────────────────────
    public static final String INTERNAL_ERROR          = "Something went wrong. Please try again.";
    public static final String VALIDATION_FAILED       = "Please check your input and try again.";
    public static final String RESOURCE_NOT_FOUND      = "The requested resource was not found.";
    public static final String DUPLICATE_ENTRY         = "This record already exists.";
}
