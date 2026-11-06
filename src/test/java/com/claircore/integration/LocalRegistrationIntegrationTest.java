package com.claircore.integration;

import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("local")
@TestPropertySource(properties = {
        "iam.registration.local-auto-confirm=true",
        "server.address=127.0.0.1"
})
@AutoConfigureMockMvc
class LocalRegistrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPlanRepository userPlanRepository;

    private static final String CREDENTIALS = """
            {"email":"local@example.test","password":"Local123!"}
            """;

    @Test
    void registrationAllowsImmediatePasswordLoginAndCreatesFreemiumWithoutMail() throws Exception {
        mvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationRequired").value(false));

        var user = userRepository.findByEmail(new EmailAddress("local@example.test")).orElseThrow();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getPassword().passwordHash()).isNotEqualTo("Local123!");
        var plan = userPlanRepository.findByUserId(
                new com.claircore.billing.domain.model.valueobjects.UserId(user.getId())).orElseThrow();
        assertThat(plan.getPlanType()).isEqualTo(PlanType.FREEMIUM);
        verifyNoInteractions(asyncNotificationService, registrationSessionRepository);

        mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isBadRequest());
    }

    @Test
    void localModeDoesNotDisablePasswordValidation() throws Exception {
        mvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"local@example.test","password":"123"}
                                """))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.existsByEmail(new EmailAddress("local@example.test"))).isFalse();
    }
}
