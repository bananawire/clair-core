package com.claircore.assistant.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyUsageTest {

    private static final Instant RESETS_AT = Instant.parse("2026-11-02T05:00:00Z");

    @Test
    void remainingIsTheAllowanceLeft() {
        var usage = new DailyUsage("FREEMIUM", 3, 10, RESETS_AT);

        assertThat(usage.remaining()).isEqualTo(7);
        assertThat(usage.isExhausted()).isFalse();
    }

    @Test
    void isExhaustedOnceTheLimitIsReached() {
        var usage = new DailyUsage("FREEMIUM", 10, 10, RESETS_AT);

        assertThat(usage.isExhausted()).isTrue();
        assertThat(usage.remaining()).isZero();
    }

    @Test
    void remainingNeverGoesNegative() {
        var usage = new DailyUsage("FREEMIUM", 12, 10, RESETS_AT);

        assertThat(usage.remaining()).isZero();
    }

    @Test
    void withOneMoreMessageCountsTheQuestionJustSent() {
        var usage = new DailyUsage("PREMIUM", 99, 100, RESETS_AT).withOneMoreMessage();

        assertThat(usage.used()).isEqualTo(100);
        assertThat(usage.isExhausted()).isTrue();
        assertThat(usage.plan()).isEqualTo("PREMIUM");
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> new DailyUsage(" ", 0, 10, RESETS_AT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DailyUsage("FREEMIUM", -1, 10, RESETS_AT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DailyUsage("FREEMIUM", 0, 10, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void localeFallsBackToSpanish() {
        assertThat(AssistantLocale.fromCode("en")).isEqualTo(AssistantLocale.EN);
        assertThat(AssistantLocale.fromCode("EN ")).isEqualTo(AssistantLocale.EN);
        assertThat(AssistantLocale.fromCode("es")).isEqualTo(AssistantLocale.ES);
        assertThat(AssistantLocale.fromCode("fr")).isEqualTo(AssistantLocale.ES);
        assertThat(AssistantLocale.fromCode(null)).isEqualTo(AssistantLocale.ES);
    }
}
