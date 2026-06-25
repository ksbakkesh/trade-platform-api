package com.tradingplatform.angelone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the TOTP generator against the official RFC 6238 test vectors
 * (Appendix B), which use the ASCII string "12345678901234567890" as the
 * secret, base32-encoded as below, with SHA1/8-digit codes truncated to 6
 * digits here since Angel One uses 6-digit codes.
 *
 * Reference vector: at T=59 (epoch), HOTP/SHA1 value is 94287082 -> last 6 digits 287082.
 */
class TotpGeneratorTest {

    // Base32 encoding of ASCII "12345678901234567890" (the RFC 6238 Appendix B test secret)
    private static final String RFC_6238_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private final TotpGenerator generator = new TotpGenerator();

    @Test
    void generatesCorrectCodeAtKnownTimestamp() {
        // RFC 6238 test vector: T=59 -> code (8-digit) is 94287082, so 6-digit is 287082
        String code = generator.generateAt(RFC_6238_SECRET, 59L);
        assertEquals("287082", code);
    }

    @Test
    void generatesSixDigitCode() {
        String code = generator.generate(RFC_6238_SECRET);
        assertEquals(6, code.length());
        assertEquals(code, code.replaceAll("[^0-9]", ""));
    }

    @Test
    void rejectsInvalidBase32Character() {
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate("INVALID1SECRET")
        );
        assertEquals("Invalid base32 character in TOTP secret: 1", ex.getMessage());
    }
}
