package com.claircore.billing.interfaces.rest.controllers;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.queries.GetSubscriptionsByUserIdQuery;
import com.claircore.billing.application.commandservices.SubscriptionCommandService;
import com.claircore.billing.application.queryservices.SubscriptionQueryService;
import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.iam.infrastructure.config.JwtAuthenticationEntryPoint;
import com.claircore.iam.infrastructure.config.SecurityConfiguration;
import com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SubscriptionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        properties = {"cors.allowed-origins=http://localhost"}
)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
class SubscriptionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionCommandService subscriptionCommandService;

    @MockitoBean
    private SubscriptionQueryService subscriptionQueryService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @Test
    void shouldRejectUnauthenticatedAccessToProtectedSubscriptionEndpoint() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440030");

        mockMvc.perform(get("/api/v1/subscriptions/user/{userId}", userId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedAccessToProtectedSubscriptionEndpoint() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440031");
        when(subscriptionQueryService.handle(any(GetSubscriptionsByUserIdQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/subscriptions/user/{userId}", userId))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowCheckoutSessionWithoutAuthentication() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440032");
        when(subscriptionCommandService.handle(any(CreateCheckoutSessionCommand.class)))
                .thenReturn("https://stripe.example/checkout-session");

        mockMvc.perform(post("/api/v1/subscriptions/checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"userId\":\"" + userId + "\"," +
                                "\"amount\":1500," +
                                "\"currency\":\"usd\"," +
                                "\"returnUrl\":\"http://frontend.local/billing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://stripe.example/checkout-session"));
    }
}
