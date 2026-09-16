package com.claircore.iam.infrastructure.tokens.jwt;

import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.shared.interfaces.rest.security.CurrentUserIdArgumentResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenQueryService tokenQueryService;

    public JwtAuthenticationFilter(TokenQueryService tokenQueryService) {
        this.tokenQueryService = tokenQueryService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else {
            jwt = request.getParameter("token");
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (tokenQueryService.isAccessTokenValid(jwt)) {
            final UUID userId = tokenQueryService.getUserIdFromToken(jwt).orElse(null);

            if (userId != null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        User.builder()
                                .username(userId.toString())
                                .password("")
                                .authorities(Collections.emptyList())
                                .build(),
                        null,
                        Collections.emptyList()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                request.setAttribute(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
