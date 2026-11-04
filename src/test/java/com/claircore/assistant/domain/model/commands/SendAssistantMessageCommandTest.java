package com.claircore.assistant.domain.model.commands;

import com.claircore.assistant.domain.model.valueobjects.AssistantLocale;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendAssistantMessageCommandTest {

    @Test
    void stripsTheContentAndDefaultsToSpanish() {
        var command = new SendAssistantMessageCommand(UUID.randomUUID(), null, "  Hola  ", null);

        assertThat(command.content()).isEqualTo("Hola");
        assertThat(command.locale()).isEqualTo(AssistantLocale.ES);
        assertThat(command.conversationId()).isNull();
    }

    @Test
    void rejectsAMissingUser() {
        assertThatThrownBy(() -> new SendAssistantMessageCommand(null, null, "Hola", AssistantLocale.ES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankContent() {
        assertThatThrownBy(() -> new SendAssistantMessageCommand(UUID.randomUUID(), null, "   ", AssistantLocale.ES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsContentAboveOneThousandCharacters() {
        assertThatThrownBy(() -> new SendAssistantMessageCommand(UUID.randomUUID(), null, "x".repeat(1001), AssistantLocale.ES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsExactlyOneThousandCharacters() {
        var command = new SendAssistantMessageCommand(UUID.randomUUID(), null, "x".repeat(1000), AssistantLocale.EN);

        assertThat(command.content()).hasSize(1000);
    }

    @Test
    void rateCommandRequiresEveryField() {
        assertThatThrownBy(() -> new RateAssistantMessageCommand(UUID.randomUUID(), UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateAssistantMessageCommand(UUID.randomUUID(), null,
                com.claircore.assistant.domain.model.valueobjects.FeedbackRating.UP))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
