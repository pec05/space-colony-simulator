package com.peccio.space_colony_simulator.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every HTTP request.
 * If a valid JWT is present in the Authorization header,
 * sets the authentication in the SecurityContext.
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        log.error(">>> JwtAuthFilter running — URI: {} | Auth header: '{}'",
                request.getRequestURI(), authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error(">>> EARLY RETURN — null={} | startsWithBearer={}",
                    authHeader == null,
                    authHeader != null && authHeader.startsWith("Bearer "));
            filterChain.doFilter(request, response);
            return;
        }

        log.error(">>> PAST Bearer check");

        String token = authHeader.substring(7);
        log.error(">>> Token length: {} | Valid: {}",
                token.length(), jwtService.isTokenValid(token));


        if (jwtService.isTokenValid(token)) {
            String username = jwtService.extractUsername(token);
            log.error(">>> Username extracted: {}", username);

            if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.error(">>> Authentication SET for user: {}", username);
            }
        }

        filterChain.doFilter(request, response);
    }
}
