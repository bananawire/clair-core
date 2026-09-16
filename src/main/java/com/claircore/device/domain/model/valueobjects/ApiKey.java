package com.claircore.device.domain.model.valueobjects;


import java.security.SecureRandom;
import java.util.Base64;

public record ApiKey(String value) {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public ApiKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank");
        }
    }

    public static ApiKey generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return new ApiKey(ENCODER.encodeToString(bytes));
    }
}
