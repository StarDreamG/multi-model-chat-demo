package com.afatguy.multimodelchat.security;

import com.afatguy.multimodelchat.openai.OpenAiDtos.OpenAiError;
import com.afatguy.multimodelchat.openai.OpenAiDtos.OpenAiErrorResponse;
import com.afatguy.multimodelchat.openai.OpenAiServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OpenAiServerApiKeyFilter extends OncePerRequestFilter {

    private final OpenAiServerProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiServerApiKeyFilter(OpenAiServerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String configuredKey = properties.apiKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            if (isLoopback(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeUnauthorized(response, "OPENAI_SERVER_API_KEY is not set (only localhost allowed)");
            return;
        }

        String token = extractBearerToken(request);
        if (token == null || token.isBlank()) {
            token = request.getHeader("X-API-Key");
        }

        if (!configuredKey.equals(token)) {
            writeUnauthorized(response, "Invalid API key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoopback(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return "127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            return null;
        }
        if (header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OpenAiErrorResponse payload = OpenAiError.invalidApiKey(message);
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}

