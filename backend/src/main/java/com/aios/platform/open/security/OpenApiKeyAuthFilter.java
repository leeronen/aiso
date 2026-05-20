package com.aios.platform.open.security;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.open.entity.OpenApiKey;
import com.aios.platform.open.service.OpenApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aios.platform.common.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class OpenApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_API_KEY = "openApiKey";

    private final OpenApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/open/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String key = resolveKey(request);
            OpenApiKey apiKey = apiKeyService.requireValid(key);
            request.setAttribute(ATTR_API_KEY, apiKey);
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            response.setStatus(e.getCode() > 0 ? e.getCode() : 401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(e.getCode(), e.getMessage()));
        }
    }

    private static String resolveKey(HttpServletRequest request) {
        String h = request.getHeader("X-API-Key");
        if (h != null && !h.isBlank()) {
            return h.trim();
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }
}
