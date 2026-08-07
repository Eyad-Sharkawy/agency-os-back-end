package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.workspaceRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.event.WorkspaceCreatedEvent;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WorkspaceResponse createWorkspace(Jwt jwt, workspaceRequest request) {
        String keycloakId = jwt.getSubject();
        log.info("Creating workspace [{}] for Keycloak user [{}]", request.name(), keycloakId);

        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> syncUserFromJwt(jwt));

        String tenantId = generateTenantId(request.name());

        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspace.setTenantId(tenantId);

        user.getWorkspaces().add(workspace);

        workspaceRepository.save(workspace);
        userRepository.save(user);

        eventPublisher.publishEvent(new WorkspaceCreatedEvent(tenantId));

        log.info("Workspace [{}] committed with schema [{}] pending provisioning", workspace.getName(), tenantId);
        return WorkspaceResponse.fromEntity(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getUserWorkspaces(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        return userRepository.findByKeycloakId(keycloakId)
                .map(user -> user.getWorkspaces().stream()
                        .map(WorkspaceResponse::fromEntity)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public void deleteUserWorkspaceByTenantId(String tenantId) {
        log.info("Deleting workspace with tenantId: {}", tenantId);

        Workspace workspace = workspaceRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with tenant id: " + tenantId));

        workspaceRepository.delete(workspace);
    }

    @Transactional
    public WorkspaceResponse updateUserWorkspaceByTenantId(String tenantId, workspaceRequest workspaceRequest) {
        Workspace workspace = workspaceRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with tenant id: " + tenantId));

        workspace.setName(workspaceRequest.name());

        Workspace savedWorkspace = workspaceRepository.save(workspace);
        return WorkspaceResponse.fromEntity(savedWorkspace);
    }

    private AppUser syncUserFromJwt(Jwt jwt) {
        AppUser newUser = new AppUser();
        newUser.setKeycloakId(jwt.getSubject());
        newUser.setUsername(jwt.getClaimAsString("preferred_username"));
        newUser.setEmail(jwt.getClaimAsString("email"));
        newUser.setFirstName(jwt.getClaimAsString("given_name"));
        newUser.setLastName(jwt.getClaimAsString("family_name"));
        return newUser;
    }

    private String generateTenantId(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        if (baseSlug.isBlank()) {
            baseSlug = "workspace";
        }

        String tenantId;
        do {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            tenantId = "tenant_" + baseSlug + "_" + suffix;
        } while (workspaceRepository.existsByTenantId(tenantId));

        return tenantId;
    }
}
