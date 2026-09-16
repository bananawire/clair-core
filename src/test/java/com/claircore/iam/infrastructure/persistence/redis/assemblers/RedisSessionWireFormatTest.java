package com.claircore.iam.infrastructure.persistence.redis.assemblers;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;
import com.claircore.iam.infrastructure.persistence.redis.documents.RegistrationSessionRedisDocument;
import com.claircore.iam.infrastructure.persistence.redis.documents.TokenSessionRedisDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the JSON that reaches Redis.
 *
 * <p>This is the phase's real control. The DDL gate covers the {@code users} table, but nothing
 * checks Redis, and sessions written before this refactor are still live: a token issued minutes
 * before a deploy must still deserialize after it, or every signed-in user is signed out. The
 * literals below are the shape the aggregates serialized to before the split — note that the
 * single-field value objects were written as nested objects, not as bare strings.
 */
class RedisSessionWireFormatTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String JTI = "22222222-2222-2222-2222-222222222222";
    private static final String SESSION_ID = "33333333-3333-3333-3333-333333333333";
    private static final Instant ISSUED_AT = Instant.parse("2026-05-16T22:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-16T23:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void tokenSessionKeepsTheFieldNamesAndNestingItWasStoredWith() throws Exception {
        var document = TokenSessionRedisAssembler.toDocumentFromDomain(new TokenSession(
                new TokenJti(JTI), new EmailAddress("user@example.com"), USER_ID,
                TokenType.ACCESS, ISSUED_AT, EXPIRES_AT));

        var node = objectMapper.readTree(objectMapper.writeValueAsString(document));

        assertThat(node.get("jti").get("jti").asText()).isEqualTo(JTI);
        assertThat(node.get("email").get("address").asText()).isEqualTo("user@example.com");
        assertThat(node.get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(node.get("type").asText()).isEqualTo("ACCESS");
        assertThat(node.get("issuedAt").asText()).isEqualTo("2026-05-16T22:00:00Z");
        assertThat(node.get("expiresAt").asText()).isEqualTo("2026-05-16T23:00:00Z");
    }

    @Test
    void aTokenSessionWrittenBeforeTheSplitStillDeserialises() throws Exception {
        String storedBeforeTheSplit = """
                {"jti":{"jti":"%s"},"email":{"address":"user@example.com"},"userId":"%s",
                 "type":"REFRESH","issuedAt":"2026-05-16T22:00:00Z","expiresAt":"2026-05-16T23:00:00Z"}
                """.formatted(JTI, USER_ID);

        var session = TokenSessionRedisAssembler.toDomainFromDocument(
                objectMapper.readValue(storedBeforeTheSplit, TokenSessionRedisDocument.class));

        assertThat(session.jti()).isEqualTo(new TokenJti(JTI));
        assertThat(session.email()).isEqualTo(new EmailAddress("user@example.com"));
        assertThat(session.userId()).isEqualTo(USER_ID);
        assertThat(session.type()).isEqualTo(TokenType.REFRESH);
        assertThat(session.issuedAt()).isEqualTo(ISSUED_AT);
        assertThat(session.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void anUnknownFieldFromANewerDeployDoesNotBreakTheRead() throws Exception {
        String fromANewerDeploy = """
                {"jti":{"jti":"%s"},"email":{"address":"user@example.com"},"userId":"%s",
                 "type":"ACCESS","issuedAt":"2026-05-16T22:00:00Z","expiresAt":"2026-05-16T23:00:00Z",
                 "deviceFingerprint":"something-added-later"}
                """.formatted(JTI, USER_ID);

        var session = TokenSessionRedisAssembler.toDomainFromDocument(
                objectMapper.readValue(fromANewerDeploy, TokenSessionRedisDocument.class));

        assertThat(session.type()).isEqualTo(TokenType.ACCESS);
    }

    @Test
    void registrationSessionKeepsTheFieldNamesAndNestingItWasStoredWith() throws Exception {
        var document = RegistrationSessionRedisAssembler.toDocumentFromDomain(new RegistrationSession(
                new RegistrationSessionId(SESSION_ID), new EmailAddress("user@example.com"),
                "$2a$10$hash", new VerificationCode("AB12-CD34"), ISSUED_AT, EXPIRES_AT));

        var node = objectMapper.readTree(objectMapper.writeValueAsString(document));

        assertThat(node.get("sessionId").get("id").asText()).isEqualTo(SESSION_ID);
        assertThat(node.get("email").get("address").asText()).isEqualTo("user@example.com");
        assertThat(node.get("passwordHash").asText()).isEqualTo("$2a$10$hash");
        assertThat(node.get("verificationCode").get("code").asText()).isEqualTo("AB12-CD34");
        assertThat(node.get("createdAt").asText()).isEqualTo("2026-05-16T22:00:00Z");
    }

    @Test
    void aRegistrationSessionWrittenBeforeTheSplitStillDeserialises() throws Exception {
        // The aggregate used to expose isExpired() as a derived "expired" property, excluded on
        // write but present in nothing that was ever stored; a stray one must still not break a read.
        String storedBeforeTheSplit = """
                {"sessionId":{"id":"%s"},"email":{"address":"user@example.com"},
                 "passwordHash":"$2a$10$hash","verificationCode":{"code":"AB12-CD34"},
                 "createdAt":"2026-05-16T22:00:00Z","expiresAt":"2026-05-16T23:00:00Z","expired":false}
                """.formatted(SESSION_ID);

        var session = RegistrationSessionRedisAssembler.toDomainFromDocument(
                objectMapper.readValue(storedBeforeTheSplit, RegistrationSessionRedisDocument.class));

        assertThat(session.sessionId()).isEqualTo(new RegistrationSessionId(SESSION_ID));
        assertThat(session.passwordHash()).isEqualTo("$2a$10$hash");
        assertThat(session.verifyCode("AB12-CD34")).isFalse(); // expired: expiresAt is in the past
    }

    @Test
    void roundTripsATokenSessionThroughTheDocumentUnchanged() throws Exception {
        var original = new TokenSession(new TokenJti(JTI), new EmailAddress("user@example.com"),
                USER_ID, TokenType.REFRESH, ISSUED_AT, EXPIRES_AT);

        String json = objectMapper.writeValueAsString(
                TokenSessionRedisAssembler.toDocumentFromDomain(original));
        var restored = TokenSessionRedisAssembler.toDomainFromDocument(
                objectMapper.readValue(json, TokenSessionRedisDocument.class));

        assertThat(restored).isEqualTo(original);
    }
}
