package com.claircore.evaluation.domain.model.valueobjects;
public record Location(
        String country
) {
    public Location {
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("country must not be null or blank");
        }
        country = country.trim();
    }
}
