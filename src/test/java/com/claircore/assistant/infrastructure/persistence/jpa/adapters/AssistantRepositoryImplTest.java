package com.claircore.assistant.infrastructure.persistence.jpa.adapters;

import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.valueobjects.FeedbackRating;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;
import com.claircore.assistant.domain.repositories.ConversationMessageRepository;
import com.claircore.assistant.domain.repositories.ConversationRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises both assistant ports through their adapters on H2. Turns are written in one
 * transaction here, so they share audit instants: the order checks prove it comes from position.
 */
@DataJpaTest
@Import({JpaAuditingConfiguration.class, ConversationRepositoryImpl.class, ConversationMessageRepositoryImpl.class})
class AssistantRepositoryImplTest {

    private static final Instant STARTED_AT = Instant.parse("2026-11-01T15:00:00Z");

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    @Test
    void savesAConversationAndFindsItOnlyForItsOwner() {
        UUID owner = UUID.randomUUID();
        var saved = conversationRepository.save(Conversation.start(owner, "Is my air OK?", STARTED_AT));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(conversationRepository.findByIdAndUserId(saved.getId(), owner)).isPresent();
        assertThat(conversationRepository.findByIdAndUserId(saved.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void pagesConversationsMostRecentlyActiveFirst() {
        UUID owner = UUID.randomUUID();
        var older = conversationRepository.save(Conversation.start(owner, "Older", STARTED_AT));
        var newer = conversationRepository.save(Conversation.start(owner, "Newer", STARTED_AT.plusSeconds(60)));
        older.recordExchange(STARTED_AT.plusSeconds(120));
        conversationRepository.save(older);
        conversationRepository.save(Conversation.start(UUID.randomUUID(), "Someone else", STARTED_AT));

        var page = conversationRepository.findByUserId(owner, 0, 10);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(Conversation::getId).containsExactly(older.getId(), newer.getId());
        assertThat(page.items().getFirst().getMessageCount()).isEqualTo(2);
    }

    @Test
    void readsTurnsBackByPositionEvenWhenTheyShareATimestamp() {
        UUID owner = UUID.randomUUID();
        var conversation = conversationRepository.save(Conversation.start(owner, "Q1", STARTED_AT));
        for (int i = 1; i <= 3; i++) {
            exchange(conversation, owner, "Q" + i, "A" + i);
        }

        assertThat(messageRepository.findByConversationId(conversation.getId()))
                .extracting(ConversationMessage::getContent)
                .containsExactly("Q1", "A1", "Q2", "A2", "Q3", "A3");
    }

    @Test
    void recentWindowKeepsTheLastTurnsInConversationOrder() {
        UUID owner = UUID.randomUUID();
        var conversation = conversationRepository.save(Conversation.start(owner, "Q1", STARTED_AT));
        for (int i = 1; i <= 3; i++) {
            exchange(conversation, owner, "Q" + i, "A" + i);
        }

        var recent = messageRepository.findRecentByConversationId(conversation.getId(), 4);

        assertThat(recent).extracting(ConversationMessage::getContent).containsExactly("Q2", "A2", "Q3", "A3");
    }

    @Test
    void countsOnlyTheUsersQuestionsSinceTheGivenInstant() {
        UUID owner = UUID.randomUUID();
        var conversation = conversationRepository.save(Conversation.start(owner, "Q1", STARTED_AT));
        exchange(conversation, owner, "Q1", "A1");
        exchange(conversation, owner, "Q2", "A2");
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);

        assertThat(messageRepository.countByUserIdAndRoleSince(owner, MessageRole.USER, startOfToday)).isEqualTo(2);
        assertThat(messageRepository.countByUserIdAndRoleSince(owner, MessageRole.USER, Instant.now().plusSeconds(60))).isZero();
        assertThat(messageRepository.countByUserIdAndRoleSince(UUID.randomUUID(), MessageRole.USER, startOfToday)).isZero();
    }

    @Test
    void storesTheFeedbackGivenToAnAnswer() {
        UUID owner = UUID.randomUUID();
        var conversation = conversationRepository.save(Conversation.start(owner, "Q", STARTED_AT));
        var answer = exchange(conversation, owner, "Q", "A").get(1);

        answer.rate(FeedbackRating.DOWN);
        messageRepository.save(answer);

        assertThat(messageRepository.findById(answer.getId()).orElseThrow().getFeedback()).isEqualTo(FeedbackRating.DOWN);
    }

    /** Mirrors the command service: positions come from the conversation, then everything is saved. */
    private List<ConversationMessage> exchange(Conversation conversation, UUID owner, String question, String answer) {
        int position = conversation.recordExchange(STARTED_AT);
        conversationRepository.save(conversation);
        return messageRepository.saveAll(List.of(
                ConversationMessage.user(conversation.getId(), owner, position, question),
                ConversationMessage.assistant(conversation.getId(), owner, position + 1, answer, "deepseek-flash", 10, 5, 100)));
    }
}
