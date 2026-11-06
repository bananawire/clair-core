package com.claircore.assistant.application.internal.usage;

import com.claircore.assistant.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;
import com.claircore.assistant.domain.repositories.ConversationMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantUsageCalculatorTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440040");

    @Mock private ConversationMessageRepository messageRepository;
    @Mock private ExternalBillingService billingService;

    @Test
    void countsFromLocalMidnightInLimaEvenWhenUtcIsAlreadyTheNextDay() {
        // 04:30 UTC on Nov 2 is still 23:30 on Nov 1 in Lima (UTC-5).
        var calculator = calculatorAt("2026-11-02T04:30:00Z");
        Instant limaMidnight = Instant.parse("2026-11-01T05:00:00Z");
        when(messageRepository.countByUserIdAndRoleSince(USER_ID, MessageRole.USER, limaMidnight)).thenReturn(4L);
        when(billingService.getAssistantDailyMessageLimit(USER_ID)).thenReturn(10);
        when(billingService.getPlanName(USER_ID)).thenReturn("FREEMIUM");

        var usage = calculator.currentUsage(USER_ID);

        assertThat(usage.used()).isEqualTo(4);
        assertThat(usage.limit()).isEqualTo(10);
        assertThat(usage.remaining()).isEqualTo(6);
        assertThat(usage.plan()).isEqualTo("FREEMIUM");
        assertThat(usage.resetsAt()).isEqualTo(Instant.parse("2026-11-02T05:00:00Z"));
    }

    @Test
    void resetsAtTheNextLocalMidnight() {
        var calculator = calculatorAt("2026-11-02T05:00:00Z");
        when(messageRepository.countByUserIdAndRoleSince(USER_ID, MessageRole.USER, Instant.parse("2026-11-02T05:00:00Z")))
                .thenReturn(0L);
        when(billingService.getAssistantDailyMessageLimit(USER_ID)).thenReturn(100);
        when(billingService.getPlanName(USER_ID)).thenReturn("PREMIUM");

        var usage = calculator.currentUsage(USER_ID);

        assertThat(usage.used()).isZero();
        assertThat(usage.resetsAt()).isEqualTo(Instant.parse("2026-11-03T05:00:00Z"));
    }

    private AssistantUsageCalculator calculatorAt(String instant) {
        var calculator = new AssistantUsageCalculator(messageRepository, billingService, "America/Lima");
        calculator.setClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        return calculator;
    }
}
