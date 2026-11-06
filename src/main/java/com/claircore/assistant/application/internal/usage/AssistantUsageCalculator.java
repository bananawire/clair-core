package com.claircore.assistant.application.internal.usage;

import com.claircore.assistant.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;
import com.claircore.assistant.domain.repositories.ConversationMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Counts today's questions against the plan's allowance. "Today" is the calendar day in the
 * configured zone (Lima by default), so the quota resets at local midnight rather than at UTC.
 */
@Component
public class AssistantUsageCalculator {

    private final ConversationMessageRepository conversationMessageRepository;
    private final ExternalBillingService externalBillingService;
    private final ZoneId zone;
    private Clock clock;

    public AssistantUsageCalculator(
            ConversationMessageRepository conversationMessageRepository,
            ExternalBillingService externalBillingService,
            @Value("${assistant.time-zone:America/Lima}") String timeZone) {
        this.conversationMessageRepository = conversationMessageRepository;
        this.externalBillingService = externalBillingService;
        this.zone = ZoneId.of(timeZone);
        this.clock = Clock.systemUTC();
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public DailyUsage currentUsage(UUID userId) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant resetsAt = today.plusDays(1).atStartOfDay(zone).toInstant();
        long used = conversationMessageRepository.countByUserIdAndRoleSince(userId, MessageRole.USER, startOfDay);
        return new DailyUsage(
                externalBillingService.getPlanName(userId),
                (int) Math.min(used, Integer.MAX_VALUE),
                externalBillingService.getAssistantDailyMessageLimit(userId),
                resetsAt);
    }

    public Instant now() {
        return clock.instant();
    }
}
