package com.claircore.notifications.domain.model.valueobjects;

import java.util.regex.Pattern;

public record EmailRecipient(String address) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public EmailRecipient {
        if (address == null || address.isBlank()) throw new IllegalArgumentException("Recipient email is required");
        if (!EMAIL_PATTERN.matcher(address).matches()) throw new IllegalArgumentException("Recipient email is invalid");
    }
}
