package dev.eyadsharkawy.agency_os_api.core.security;

import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private static final Pattern TENANT_TOPIC_PATTERN =
      Pattern.compile("^/topic/(tenant_[a-z0-9_]+)/.*");
  private final JwtDecoder jwtDecoder;
  private final WorkspaceRepository workspaceRepository;

  @Override
  @Nullable
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      authenticateConnectFrame(accessor);
    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      authorizeSubscriptionFrame(accessor);
    }

    return message;
  }

  private void authenticateConnectFrame(StompHeaderAccessor accessor) {
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    log.debug(
        "WebSocket STOMP CONNECT frame received. Auth header present: {}", authHeader != null);

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      try {
        Jwt jwt = jwtDecoder.decode(token);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());
        accessor.setUser(authentication);
      } catch (Exception e) {
        log.error("WebSocket JWT validation failed", e);
        throw new IllegalArgumentException("Invalid JWT token");
      }
    } else {
      log.warn("WebSocket CONNECT frame rejected: Missing Bearer token");
      throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
  }

  private void authorizeSubscriptionFrame(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null) {
      return;
    }

    Matcher matcher = TENANT_TOPIC_PATTERN.matcher(destination);
    if (matcher.matches()) {
      String tenantId = matcher.group(1);

      Authentication authentication = (Authentication) accessor.getUser();
      if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
        log.warn("WebSocket SUBSCRIBE frame rejected: Session is unauthenticated");
        throw new IllegalArgumentException("Unauthenticated session");
      }

      String keycloakUserId = jwt.getSubject();
      log.debug(
          "User [{}] attempting to subscribe to destination [{}] for tenant [{}]",
          keycloakUserId,
          destination,
          tenantId);

      boolean isAuthorized = workspaceRepository.isUserMemberOfTenant(keycloakUserId, tenantId);
      if (!isAuthorized) {
        log.warn(
            "WebSocket SUBSCRIBE rejected: User [{}] is not a member of tenant [{}]",
            keycloakUserId,
            tenantId);
        throw new IllegalArgumentException("Access denied to tenant topic: " + tenantId);
      }

      log.info(
          "WebSocket SUBSCRIBE authorized for user [{}] to tenant topic [{}]",
          keycloakUserId,
          destination);
    }
  }
}
