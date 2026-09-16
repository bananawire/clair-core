package com.claircore.device.domain.model.valueobjects;


import java.security.SecureRandom;

public record ClaimToken(String value) {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public ClaimToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Claim token must not be null or blank");
        }

        // GitHub-like short code, used once during claim.
        // Also accept legacy base64-url tokens to avoid breaking existing persisted data.
        boolean isShortCode = value.matches("^[A-Z0-9]{4}-[A-Z0-9]{4}$");
        boolean isLegacyBase64Url = value.matches("^[A-Za-z0-9_-]{20,}$");
        if (!isShortCode && !isLegacyBase64Url) {
            throw new IllegalArgumentException("Claim token must match AB45-F3B1");
        }
    }

    public static ClaimToken generate() {
        byte[] bytes = new byte[4];
        SECURE_RANDOM.nextBytes(bytes);
        String hex = toUpperHex(bytes);
        return new ClaimToken(hex.substring(0, 4) + "-" + hex.substring(4, 8));
    }

    private static String toUpperHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        final char[] alphabet = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = alphabet[v >>> 4];
            out[i * 2 + 1] = alphabet[v & 0x0F];
        }
        return new String(out);
    }
}
