package com.claircore.notifications.domain.model.valueobjects;

public record EmailContent(String html) {
    public EmailContent {
        if (html == null || html.isBlank()) throw new IllegalArgumentException("Email content is required");
    }
}
