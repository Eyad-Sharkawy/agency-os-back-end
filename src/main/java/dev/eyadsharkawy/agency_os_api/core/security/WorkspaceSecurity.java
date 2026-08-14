package dev.eyadsharkawy.agency_os_api.core.security;

import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("workspaceSecurity")
@RequiredArgsConstructor
public class WorkspaceSecurity {

  private final UserWorkspaceRepository userWorkspaceRepository;

  public boolean hasRole(String... allowedRoles) {
    String tenantId = TenantContextHolder.getTenantId();
    return hasRoleInTenant(tenantId, allowedRoles);
  }

  public boolean hasRoleInTenant(String tenantId, String... allowedRoles) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return false;
    }

    String keycloakId = jwt.getSubject();
    if (tenantId == null) {
      return false;
    }

    return userWorkspaceRepository
        .findRoleByKeycloakIdAndTenantId(keycloakId, tenantId)
        .map(role -> Arrays.asList(allowedRoles).contains(role.name()))
        .orElse(false);
  }
}
