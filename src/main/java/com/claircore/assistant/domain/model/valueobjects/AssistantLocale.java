package com.claircore.assistant.domain.model.valueobjects;

/** Language the assistant answers in. Spanish unless the client asks for English. */
public enum AssistantLocale {
    ES,
    EN;

    /** Resolves an ISO code such as {@code es} or {@code en}; blank or unknown codes fall back to Spanish. */
    public static AssistantLocale fromCode(String code) {
        if (code == null || code.isBlank()) return ES;
        return "en".equalsIgnoreCase(code.trim()) ? EN : ES;
    }
}
