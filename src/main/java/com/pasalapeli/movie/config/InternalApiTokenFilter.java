package com.pasalapeli.movie.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Protege los endpoints de stock (/descontar, /reponer) con un token interno
 * compartido solo entre movie-service y ticket-service (red privada del compose).
 * Cualquier llamante externo sin el header X-Internal-Token recibe 403.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalApiTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    @Value("${app.internal-api-token:dev-internal-token}")
    private String expectedToken;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.endsWith("/descontar") || path.endsWith("/reponer"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedToken == null || expectedToken.isBlank() || !expectedToken.equals(provided)) {
            log.warn("Acceso denegado a stock sin token interno valido: {} {}", request.getMethod(), request.getRequestURI());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getOutputStream().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Se requiere token interno de servicio para operar el stock",
                    "timestamp", java.time.LocalDateTime.now().toString()
            )));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
