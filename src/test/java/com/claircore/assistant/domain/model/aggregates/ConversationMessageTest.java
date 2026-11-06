package com.claircore.assistant.domain.model.aggregates;

import com.claircore.assistant.domain.model.valueobjects.FeedbackRating;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationMessageTest {

    private static final UUID CONVERSATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void userMessageIsStrippedAndHasNoModelData() {
        var message = ConversationMessage.user(CONVERSATION_ID, USER_ID, 0, "  Is my air OK?  ");

        assertThat(message.getRole()).isEqualTo(MessageRole.USER);
        assertThat(message.getContent()).isEqualTo("Is my air OK?");
        assertThat(message.getModel()).isNull();
        assertThat(message.getPromptTokens()).isNull();
        assertThat(message.isFromAssistant()).isFalse();
    }

    @Test
    void assistantMessageKeepsModelTokensAndLatency() {
        var message = ConversationMessage.assistant(CONVERSATION_ID, USER_ID, 1, "Yes, it is.", "deepseek-flash", 250, 40, 1200);

        assertThat(message.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(message.getModel()).isEqualTo("deepseek-flash");
        assertThat(message.getPromptTokens()).isEqualTo(250);
        assertThat(message.getCompletionTokens()).isEqualTo(40);
        assertThat(message.getLatencyMs()).isEqualTo(1200);
        assertThat(message.getFeedback()).isNull();
    }

    @Test
    void userMessageRejectsBlankContent() {
        assertThatThrownBy(() -> ConversationMessage.user(CONVERSATION_ID, USER_ID, 0, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userMessageRejectsContentAboveTheLimit() {
        String tooLong = "x".repeat(ConversationMessage.CONTENT_MAX_LENGTH + 1);

        assertThatThrownBy(() -> ConversationMessage.user(CONVERSATION_ID, USER_ID, 0, tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assistantAnswersMayBeLongerThanTheQuestionLimit() {
        String longAnswer = "x".repeat(ConversationMessage.CONTENT_MAX_LENGTH * 3);

        var message = ConversationMessage.assistant(CONVERSATION_ID, USER_ID, 1, longAnswer, "m", null, null, 10);

        assertThat(message.getContent()).hasSize(longAnswer.length());
    }

    @Test
    void rateRecordsAndReplacesTheVerdictOnAssistantMessages() {
        var message = ConversationMessage.assistant(CONVERSATION_ID, USER_ID, 1, "Answer", "m", 1, 1, 1);

        message.rate(FeedbackRating.DOWN);
        message.rate(FeedbackRating.UP);

        assertThat(message.getFeedback()).isEqualTo(FeedbackRating.UP);
    }

    @Test
    void rateIsRejectedOnUserMessages() {
        var message = ConversationMessage.user(CONVERSATION_ID, USER_ID, 0, "Question");

        assertThatThrownBy(() -> message.rate(FeedbackRating.UP)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reconstituteRejectsFeedbackOnAUserMessage() {
        assertThatThrownBy(() -> ConversationMessage.reconstitute(UUID.randomUUID(), CONVERSATION_ID, USER_ID, 0,
                MessageRole.USER, "Question", null, null, null, null, FeedbackRating.UP, Instant.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void belongsToOnlyItsAuthor() {
        var message = ConversationMessage.user(CONVERSATION_ID, USER_ID, 0, "Question");

        assertThat(message.belongsTo(USER_ID)).isTrue();
        assertThat(message.belongsTo(UUID.randomUUID())).isFalse();
    }
}
