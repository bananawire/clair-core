package com.claircore.nino.domain.model.valueobjects;

/** A plain-language El Niño alert and who wrote it: the language model or the pre-written fallback. */
public record NinoAlert(String message, String source) {

    public static final String SOURCE_MODEL = "deepseek";
    public static final String SOURCE_FALLBACK = "fallback";

    public NinoAlert {
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message is required");
        if (!SOURCE_MODEL.equals(source) && !SOURCE_FALLBACK.equals(source)) {
            throw new IllegalArgumentException("Source must be deepseek or fallback");
        }
    }
}
