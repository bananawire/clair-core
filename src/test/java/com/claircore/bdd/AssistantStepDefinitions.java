package com.claircore.bdd;

import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelClient;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelMessage;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelReply;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The language model is mocked: these scenarios pin the assistant's rules, not DeepSeek's wording. */
public class AssistantStepDefinitions extends AbstractCucumberSteps {

    @Autowired
    private LanguageModelClient languageModelClient;

    private static JsonNode lastReply;

    @Dado("que un usuario con plan {string} ha iniciado sesión")
    public void usuarioConPlan(String plan) {
        lastReply = null;
        registerAndSignIn(uniqueEmail("assistant"), "Clair@2026");
        assignPlan(state.userId, plan);
    }

    @Y("el asistente responderá {string}")
    public void asistenteResponde(String answer) {
        when(languageModelClient.complete(anyList()))
                .thenReturn(new LanguageModelReply(answer, "deepseek-flash", 200, 20, 850));
    }

    @Y("el asistente no está disponible")
    public void asistenteNoDisponible() {
        when(languageModelClient.complete(anyList()))
                .thenThrow(new AssistantUnavailableException("Assistant is temporarily unavailable"));
    }

    @Y("ya envió {int} mensajes al asistente hoy")
    public void yaEnvioMensajes(int count) {
        UUID conversationId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO assistant_conversations (id, user_id, title, last_message_at, message_count, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", conversationId, state.userId, "Earlier", now, count, now, now);
        for (int position = 0; position < count; position++) {
            jdbc.update("INSERT INTO assistant_messages (id, conversation_id, user_id, position, role, content, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, 'USER', ?, ?, ?)", UUID.randomUUID(), conversationId, state.userId, position,
                    "Earlier question " + position, now, now);
        }
    }

    @Cuando("le pregunta al asistente {string}")
    public void preguntaAlAsistente(String question) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", question);
        body.put("locale", "es");
        var response = post("/api/v1/assistant/messages", body);
        lastReply = response.getStatusCode().value() == 200 ? json(response) : null;
    }

    @Y("la respuesta del asistente es {string}")
    public void respuestaDelAsistente(String answer) {
        assertThat(lastReply).isNotNull();
        assertThat(lastReply.get("assistantMessage").get("content").asText()).isEqualTo(answer);
        assertThat(lastReply.get("assistantMessage").get("role").asText()).isEqualTo("ASSISTANT");
    }

    @Y("al usuario le quedan {int} de {int} mensajes del asistente hoy")
    public void cupoRestante(int remaining, int limit) {
        JsonNode usage = json(get("/api/v1/assistant/usage"));
        assertThat(usage.get("remaining").asInt()).isEqualTo(remaining);
        assertThat(usage.get("limit").asInt()).isEqualTo(limit);
    }

    @Y("el asistente no fue consultado")
    public void asistenteNoConsultado() {
        verify(languageModelClient, never()).complete(anyList());
    }

    @Y("el usuario no tiene conversaciones con el asistente")
    public void sinConversaciones() {
        assertThat(json(get("/api/v1/assistant/conversations")).get("totalElements").asLong()).isZero();
    }

    @Y("el contexto enviado al asistente menciona el espacio {string}")
    public void contextoMencionaEspacio(String spaceName) {
        assertThat(systemPrompt()).contains("in space \"" + spaceName + "\"");
    }

    @Y("el contexto enviado al asistente no incluye el correo del usuario")
    public void contextoSinCorreo() {
        assertThat(systemPrompt()).doesNotContain(state.email).doesNotContain(state.userId.toString());
    }

    @Cuando("califica la respuesta del asistente con {string}")
    public void calificaRespuesta(String rating) {
        assertThat(lastReply).isNotNull();
        String messageId = lastReply.get("assistantMessage").get("id").asText();
        put("/api/v1/assistant/messages/" + messageId + "/feedback", Map.of("rating", rating));
    }

    @Y("la conversación muestra la respuesta del asistente calificada con {string}")
    public void conversacionConCalificacion(String rating) {
        String conversationId = lastReply.get("conversationId").asText();
        JsonNode messages = json(get("/api/v1/assistant/conversations/" + conversationId + "/messages"));
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).get("role").asText()).isEqualTo("USER");
        assertThat(messages.get(1).get("feedback").asText()).isEqualTo(rating);
    }

    @SuppressWarnings("unchecked")
    private String systemPrompt() {
        ArgumentCaptor<List<LanguageModelMessage>> prompt = ArgumentCaptor.forClass(List.class);
        verify(languageModelClient).complete(prompt.capture());
        return prompt.getValue().getFirst().content();
    }
}
