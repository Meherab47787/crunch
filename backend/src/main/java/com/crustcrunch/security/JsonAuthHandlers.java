package com.crustcrunch.security;

import com.crustcrunch.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/** Writes 401/403 responses in the same {@link ApiError} shape as the rest of the API. */
@Component
public class JsonAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public JsonAuthHandlers(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        write(req, res, HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        write(req, res, HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    private void write(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String msg) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(),
                new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), msg, req.getRequestURI(), null));
    }
}
