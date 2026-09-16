package com.claircore.device.domain.model.valueobjects;

public record HardwareId(String value) {
    public HardwareId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }

        // Supported formats:
        // - Factory/edge: CLAIR-0KBG (4 chars, uppercase alphanumeric)
        // - Legacy:       HW-0001  (4 digits)
        boolean isClairFormat = value.matches("^CLAIR-[0-9A-Z]{4}$");
        boolean isLegacyFormat = value.matches("^HW-\\d{4}$");
        if (!isClairFormat && !isLegacyFormat) {
            throw new IllegalArgumentException("Hardware ID must match CLAIR-0KBG or HW-0001");
        }
    }
}
