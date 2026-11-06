package com.claircore.assistant.domain.model.aggregates;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationTest {

    private static final Instant STARTED_AT = Instant.parse("2026-11-01T15:00:00Z");

    @Test
    void startTitlesTheConversationAfterTheFirstQuestion() {
        UUID userId = UUID.randomUUID();

        var conversation = Conversation.start(userId, "  Why is the CO2\nhigh   today?  ", STARTED_AT);

        assertThat(conversation.getId()).isNotNull();
        assertThat(conversation.getUserId()).isEqualTo(userId);
        assertThat(conversation.getTitle()).isEqualTo("Why is the CO2 high today?");
        assertThat(conversation.getLastMessageAt()).isEqualTo(STARTED_AT);
        assertThat(conversation.getCreatedAt()).isNull();
    }

    @Test
    void startShortensLongQuestionsToTheTitleLimit() {
        var conversation = Conversation.start(UUID.randomUUID(), "a".repeat(200), STARTED_AT);

        assertThat(conversation.getTitle()).hasSize(Conversation.TITLE_MAX_LENGTH).endsWith("...");
    }

    @Test
    void startRejectsABlankFirstQuestion() {
        assertThatThrownBy(() -> Conversation.start(UUID.randomUUID(), "   ", STARTED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startRejectsAMissingUser() {
        assertThatThrownBy(() -> Conversation.start(null, "Hello", STARTED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordExchangeHandsOutConsecutivePositionsForQuestionAndAnswer() {
        var conversation = Conversation.start(UUID.randomUUID(), "Hello", STARTED_AT);

        int first = conversation.recordExchange(STARTED_AT);
        int second = conversation.recordExchange(STARTED_AT.plusSeconds(30));

        assertThat(first).isZero();
        assertThat(second).isEqualTo(2);
        assertThat(conversation.getMessageCount()).isEqualTo(4);
    }

    @Test
    void recordExchangeOnlyMovesTheLastActivityForward() {
        var conversation = Conversation.start(UUID.randomUUID(), "Hello", STARTED_AT);

        conversation.recordExchange(STARTED_AT.plusSeconds(60));
        conversation.recordExchange(STARTED_AT.minusSeconds(60));

        assertThat(conversation.getLastMessageAt()).isEqualTo(STARTED_AT.plusSeconds(60));
    }

    @Test
    void reconstituteRejectsANegativeMessageCount() {
        assertThatThrownBy(() -> Conversation.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "Hello",
                STARTED_AT, -1, STARTED_AT, STARTED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void belongsToOnlyItsOwner() {
        UUID owner = UUID.randomUUID();
        var conversation = Conversation.start(owner, "Hello", STARTED_AT);

        assertThat(conversation.belongsTo(owner)).isTrue();
        assertThat(conversation.belongsTo(UUID.randomUUID())).isFalse();
    }
}
