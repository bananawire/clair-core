package com.claircore.iam.domain.model.valueobjects;

public record EmailAddress(String address) {
    public EmailAddress {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
    }
}
