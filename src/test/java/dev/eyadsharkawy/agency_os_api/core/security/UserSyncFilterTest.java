package dev.eyadsharkawy.agency_os_api.core.security;

import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.service.UserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UserSyncFilterTest {

  @Mock private ObjectProvider<UserSyncService> userSyncServiceProvider;
  @Mock private UserSyncService userSyncService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private UserSyncFilter userSyncFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    userSyncFilter = new UserSyncFilter(userSyncServiceProvider);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_WhenAuthenticatedWithJwtAndServiceAvailable_ShouldSyncUserAndProceed()
      throws ServletException, IOException {
    Jwt jwt =
        new Jwt(
            "token",
            null,
            null,
            Map.of("alg", "none"),
            Map.of("sub", "kc-123", "preferred_username", "alice"));
    JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(authToken);

    when(userSyncServiceProvider.getIfAvailable()).thenReturn(userSyncService);
    when(userSyncService.getOrSyncUser(jwt)).thenReturn(new AppUser());

    userSyncFilter.doFilterInternal(request, response, filterChain);

    verify(userSyncService, times(1)).getOrSyncUser(jwt);
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void doFilterInternal_WhenServiceNotAvailable_ShouldProceedWithoutSync()
      throws ServletException, IOException {
    Jwt jwt =
        new Jwt(
            "token",
            null,
            null,
            Map.of("alg", "none"),
            Map.of("sub", "kc-123", "preferred_username", "alice"));
    JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(authToken);

    when(userSyncServiceProvider.getIfAvailable()).thenReturn(null);

    userSyncFilter.doFilterInternal(request, response, filterChain);

    verify(userSyncService, never()).getOrSyncUser(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void doFilterInternal_WhenNotJwtAuthenticated_ShouldProceedWithoutSync()
      throws ServletException, IOException {
    userSyncFilter.doFilterInternal(request, response, filterChain);

    verify(userSyncService, never()).getOrSyncUser(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }
}
