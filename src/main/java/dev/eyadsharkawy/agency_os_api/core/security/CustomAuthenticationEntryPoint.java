package dev.eyadsharkawy.agency_os_api.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    log.warn(
        "Unauthorized access attempt to {}: {}",
        request.getRequestURI(),
        authException.getMessage());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader(
        "WWW-Authenticate",
        "Bearer error=\"unauthorized\", error_description=\""
            + (authException.getMessage() != null
                ? authException.getMessage()
                : "Full authentication is required")
            + "\"");

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Authentication is required to access this endpoint. Please provide a valid Bearer token.");
    problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
    problemDetail.setProperty("timestamp", Instant.now().toString());

    response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
  }
}
