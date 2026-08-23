package dev.eyadsharkawy.agency_os_api.core.security;

import dev.eyadsharkawy.agency_os_api.global.user.service.UserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class UserSyncFilter extends OncePerRequestFilter {

  private final ObjectProvider<UserSyncService> userSyncServiceProvider;

  public UserSyncFilter(ObjectProvider<UserSyncService> userSyncServiceProvider) {
    this.userSyncServiceProvider = userSyncServiceProvider;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
      UserSyncService userSyncService = userSyncServiceProvider.getIfAvailable();
      if (userSyncService != null) {
        Jwt jwt = jwtAuthToken.getToken();
        try {
          userSyncService.getOrSyncUser(jwt);
        } catch (Exception e) {
          log.warn("Failed to automatically synchronize user from JWT: {}", e.getMessage());
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
