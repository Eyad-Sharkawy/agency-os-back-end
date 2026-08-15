package dev.eyadsharkawy.agency_os_api.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class SecurityExceptionHandlerTest {

  private ObjectMapper objectMapper;
  private CustomAuthenticationEntryPoint authenticationEntryPoint;
  private CustomAccessDeniedHandler accessDeniedHandler;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    authenticationEntryPoint = new CustomAuthenticationEntryPoint(objectMapper);
    accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
  }

  @Test
  @DisplayName("CustomAuthenticationEntryPoint should return 401 with ProblemDetail JSON body")
  void testAuthenticationEntryPointCommence() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/workspaces");
    MockHttpServletResponse response = new MockHttpServletResponse();

    InsufficientAuthenticationException exception =
        new InsufficientAuthenticationException(
            "Full authentication is required to access this resource");

    authenticationEntryPoint.commence(request, response, exception);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getHeader("WWW-Authenticate")).contains("Bearer error=\"unauthorized\"");

    JsonNode json = objectMapper.readTree(response.getContentAsString());
    assertThat(json.get("status").asInt()).isEqualTo(401);
    assertThat(json.get("detail").asText())
        .isEqualTo(
            "Authentication is required to access this endpoint. Please provide a valid Bearer token.");
    assertThat(json.get("instance").asText()).isEqualTo("/api/v1/workspaces");
    assertThat(json.get("properties").get("timestamp")).isNotNull();
  }

  @Test
  @DisplayName("CustomAccessDeniedHandler should return 403 with ProblemDetail JSON body")
  void testAccessDeniedHandlerHandle() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/workspaces/admin-only");
    MockHttpServletResponse response = new MockHttpServletResponse();

    AccessDeniedException exception = new AccessDeniedException("Access is denied");

    accessDeniedHandler.handle(request, response, exception);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");

    JsonNode json = objectMapper.readTree(response.getContentAsString());
    assertThat(json.get("status").asInt()).isEqualTo(403);
    assertThat(json.get("detail").asText())
        .isEqualTo("You do not have permission to access this resource.");
    assertThat(json.get("instance").asText()).isEqualTo("/api/v1/workspaces/admin-only");
    assertThat(json.get("properties").get("timestamp")).isNotNull();
  }
}
