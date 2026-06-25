package com.tradingplatform.angelone;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Generates RFC 6238 TOTP codes from a base32 secret - the same kind of code
 * your authenticator app would show. Angel One requires this on every login call.
 *
 * No external TOTP library needed; this is ~40 lines of straightforward HMAC-SHA1.
 */
@Component
public class TotpGenerator {

    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;

    /** Generates the current 6-digit TOTP code for the given base32 secret. */
    public String generate(String base32Secret) {
        return generateAt(base32Secret, Instant.now().getEpochSecond());
    }

    String generateAt(String base32Secret, long epochSeconds) {
        byte[] key = base32Decode(base32Secret);
        long counter = epochSeconds / TIME_STEP_SECONDS;

        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP code", e);
        }
    }

    /** Minimal RFC 4648 base32 decoder (no padding handling needed for TOTP secrets). */
    private byte[] base32Decode(String base32) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String cleaned = base32.trim().toUpperCase().replace("=", "");

        int byteCount = cleaned.length() * 5 / 8;
        byte[] result = new byte[byteCount];

        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : cleaned.toCharArray()) {
            int val = alphabet.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Invalid base32 character in TOTP secret: " + c);
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                result[index++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }

        return result;
    }
}
