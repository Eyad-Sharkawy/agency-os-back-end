package dev.eyadsharkawy.agency_os_api.core.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class TenantSecurityFilterTest {

  @Mock private WorkspaceRepository workspaceRepository;

  @Spy
  private ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

  @InjectMocks private TenantSecurityFilter tenantSecurityFilter;

  @Mock private FilterChain filterChain;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    TenantContextHolder.clear();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Should bypass filter for public API endpoints")
  void testPublicApiBypass() throws ServletException, IOException {
    request.setRequestURI("/api/v1/public/login");

    tenantSecurityFilter.doFilter(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  @DisplayName("Should bypass filter for global endpoints")
  void testGlobalEndpointBypass() throws ServletException, IOException {
    request.setRequestURI("/api/v1/workspaces");

    tenantSecurityFilter.doFilter(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  @DisplayName("Should return 400 Bad Request when X-Tenant-ID header is missing")
  void testMissingTenantIdHeader() throws ServletException, IOException {
    request.setRequestURI("/api/v1/projects"); // Secure path

    tenantSecurityFilter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("Missing required header: X-Tenant-ID");
  }

  @Test
  @DisplayName("Should return 403 Forbidden when user is not member of tenant")
  void testUserNotMemberOfTenant() throws ServletException, IOException {
    request.setRequestURI("/api/v1/projects");
    request.addHeader("X-Tenant-ID", "tenant-1");

    Jwt jwt = mock(Jwt.class);
    when(jwt.getSubject()).thenReturn("user-123");
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(jwt, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    when(workspaceRepository.isUserMemberOfTenant("user-123", "tenant-1")).thenReturn(false);

    tenantSecurityFilter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(response.getContentAsString()).contains("Access denied to workspace: tenant-1");
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  @DisplayName("Should proceed with filter chain and set tenant context when request is authorized")
  void testAuthorizedRequest() throws ServletException, IOException {
    request.setRequestURI("/api/v1/projects");
    request.addHeader("X-Tenant-ID", "tenant-1");

    Jwt jwt = mock(Jwt.class);
    when(jwt.getSubject()).thenReturn("user-123");
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(jwt, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    when(workspaceRepository.isUserMemberOfTenant("user-123", "tenant-1")).thenReturn(true);

    // Verify tenant ID is set during doFilter execution inside the filter chain
    doAnswer(
            invocation -> {
              assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-1");
              return null;
            })
        .when(filterChain)
        .doFilter(request, response);

    tenantSecurityFilter.doFilter(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    // Verify context is cleared post-execution
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }
}
