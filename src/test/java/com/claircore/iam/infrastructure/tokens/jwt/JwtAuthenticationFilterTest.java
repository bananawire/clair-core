package com.claircore.iam.infrastructure.tokens.jwt;

import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.shared.interfaces.rest.security.CurrentUserIdArgumentResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenQueryService tokenQueryService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenQueryService);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPopulateSecurityContextWhenBearerTokenIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        when(tokenQueryService.isAccessTokenValid("access-token")).thenReturn(true);
        when(tokenQueryService.getUserIdFromToken("access-token")).thenReturn(Optional.of(userId));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userId.toString(), authentication.getName());
        assertEquals(userId, request.getAttribute(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReadTokenFromQueryParameterWhenHeaderIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        when(tokenQueryService.isAccessTokenValid("access-token")).thenReturn(true);
        when(tokenQueryService.getUserIdFromToken("access-token")).thenReturn(Optional.of(userId));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("token", "access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(userId.toString(), SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void shouldPassThroughWhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenQueryService.isAccessTokenValid("invalid-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenRefreshTokenIsProvidedInsteadOfAccessToken() throws Exception {
        when(tokenQueryService.isAccessTokenValid("refresh-token")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenNoTokenIsPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
