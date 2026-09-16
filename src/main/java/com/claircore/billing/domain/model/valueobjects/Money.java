package com.claircore.billing.domain.model.valueobjects;

public record Money(Long amount, String currency) {
    public Money {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be empty");
        }
    }
}
