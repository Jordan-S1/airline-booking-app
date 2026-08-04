package com.airlinebookingsystem.security;

import com.airlinebookingsystem.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Returns 401 when a request arrives without usable credentials.
 *
 * <p>Spring Security's default for an API with no login mechanism configured
 * is {@code Http403ForbiddenEntryPoint}, which made every failure look alike:
 * a missing token, an expired one, one signed with a retired key, and a valid
 * token whose role simply is not allowed all came back as 403. A client
 * cannot act on that — "sign in again" and "you may not do this" need
 * different responses, and clearing the session on the latter would sign
 * people out for clicking a link meant for admins.
 *
 * <p>With this in place 401 means the credentials are absent or no longer
 * valid, and 403 is left to mean the caller is known but not permitted, which
 * is what {@code AuthorizationDeniedException} already produces.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Authentication required")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
