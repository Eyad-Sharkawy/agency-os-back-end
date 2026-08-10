package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceMemberResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceMemberUpdateRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspaceId;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.event.WorkspaceCreatedEvent;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final ClientUserRepository clientUserRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public WorkspaceResponse createWorkspace(Jwt jwt, WorkspaceRequest request) {
        String keycloakId = jwt.getSubject();
        log.info("Creating workspace [{}] for Keycloak user [{}]", request.name(), keycloakId);

        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> syncUserFromJwt(jwt));

        String tenantId = generateTenantId(request.name());

        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspace.setTenantId(tenantId);
        workspace.setContactEmail(user.getEmail());

        workspaceRepository.save(workspace);

        UserWorkspace membership = new UserWorkspace();
        membership.setUser(user);
        membership.setWorkspace(workspace);
        membership.setRole(WorkspaceRole.OWNER);

        user.getUserWorkspaces().add(membership);
        userRepository.save(user);

        eventPublisher.publishEvent(new WorkspaceCreatedEvent(tenantId));

        log.info("Workspace [{}] committed with schema [{}] pending provisioning", workspace.getName(), tenantId);
        return WorkspaceResponse.fromEntity(workspace, membership.getRole());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getUserWorkspaces(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        return userRepository.findByKeycloakId(keycloakId)
                .map(user -> user.getUserWorkspaces().stream()
                        .map(userWorkspace -> WorkspaceResponse.fromEntity(userWorkspace.getWorkspace(), userWorkspace.getRole()))
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public void deleteUserWorkspaceByTenantId(String tenantId) {
        log.info("Deleting workspace with tenantId: {}", tenantId);

        Workspace workspace = findWorkspaceByTenantIdOrThrow(tenantId);

        workspaceRepository.delete(workspace);
    }

    @Transactional
    public WorkspaceResponse updateUserWorkspaceByTenantId(String tenantId, WorkspaceRequest workspaceRequest) {
        Workspace workspace = findWorkspaceByTenantIdOrThrow(tenantId);

        workspace.setName(workspaceRequest.name());

        Workspace savedWorkspace = workspaceRepository.save(workspace);
        return WorkspaceResponse.fromEntity(savedWorkspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getWorkspaceMembers(String tenantId) {
        Workspace workspace = findWorkspaceByTenantIdOrThrow(tenantId);

        return workspace.getUserWorkspaces().stream()
                .map(WorkspaceMemberResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void updateWorkspaceMember(Jwt jwt, String tenantId, UUID userId, WorkspaceMemberUpdateRequest request) {
        Workspace workspace = findWorkspaceByTenantIdOrThrow(tenantId);

        String requesterKeycloakId = jwt.getSubject();

        WorkspaceRole requesterRole = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(requesterKeycloakId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + tenantId));


        UserWorkspace membership = findUserWorkspaceByIdOrThrow(new UserWorkspaceId(userId, workspace.getId()));

        if (membership.getUser().getKeycloakId().equals(requesterKeycloakId))
            throw new IllegalArgumentException("You cannot modify your own role.");

        if (request.role() == WorkspaceRole.OWNER)
            throw new IllegalArgumentException("To change the owner, please use the ownership transfer flow.");

        if (request.role() == WorkspaceRole.CLIENT || membership.getRole() == WorkspaceRole.CLIENT)
            if (requesterRole != WorkspaceRole.OWNER)
                throw new AccessDeniedException("Only the workspace OWNER can modify CLIENT role associations.");

        // Check 4: Admins cannot modify other Admins or assign the ADMIN role
        if (requesterRole == WorkspaceRole.ADMIN) {
            if (request.role() == WorkspaceRole.ADMIN) {
                throw new AccessDeniedException("Admins cannot promote users to the ADMIN role.");
            }
            if (membership.getRole() == WorkspaceRole.ADMIN) {
                throw new AccessDeniedException("Admins do not have permission to modify other Admins.");
            }
        }

        if (request.role() == WorkspaceRole.CLIENT) {
            if (request.clientId() == null)
                throw new IllegalArgumentException("Client ID is required when role is CLIENT.");

            TenantContextHolder.setTenantId(tenantId);
            try {
                Client client = clientRepository.findById(request.clientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));

                ClientUser clientUser = new ClientUser();
                clientUser.setUserId(membership.getUser().getKeycloakId());
                clientUser.setClient(client);
                clientUserRepository.save(clientUser);
            } finally {
                TenantContextHolder.clear();
            }
        } else if (membership.getRole() == WorkspaceRole.CLIENT) {
            TenantContextHolder.setTenantId(tenantId);
            try {
                clientUserRepository.deleteById(membership.getUser().getKeycloakId());
            } finally {
                TenantContextHolder.clear();
            }
        }

        membership.setRole(request.role());
        userWorkspaceRepository.save(membership);
    }

    @Transactional
    public void removeWorkspaceMember(String requesterKeycloakId, String tenantId, UUID userId) {
        Workspace workspace = workspaceRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + tenantId));
        WorkspaceRole requesterRole = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(requesterKeycloakId, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Access Denied: Requester is not a member."));
        UserWorkspace membership = userWorkspaceRepository.findById(new UserWorkspaceId(userId, workspace.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found."));
        if (membership.getUser().getKeycloakId().equals(requesterKeycloakId)) {
            throw new IllegalArgumentException("You cannot remove yourself from the workspace.");
        }
        // Check 2: Only OWNER can remove CLIENTs
        if (membership.getRole() == WorkspaceRole.CLIENT && requesterRole != WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Only the workspace OWNER can remove CLIENT users.");
        }

        // Check 3: Admins cannot remove other Admins
        if (membership.getRole() == WorkspaceRole.ADMIN && requesterRole != WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Admins do not have permission to remove other Admins.");
        }
        if (membership.getRole() == WorkspaceRole.CLIENT) {
            TenantContextHolder.setTenantId(tenantId);
            try {
                clientUserRepository.deleteById(membership.getUser().getKeycloakId());
            } finally {
                TenantContextHolder.clear();
            }
        }
        userWorkspaceRepository.delete(membership);
    }

    @Transactional
    public void transferOwnership(String currentOwnerKeycloakId, String tenantId, UUID newOwnerUserId) {
        Workspace workspace = workspaceRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + tenantId));

        UserWorkspace oldOwnerMembership = userWorkspaceRepository.findById(
                new UserWorkspaceId(workspace.getUserWorkspaces().stream()
                        .filter(uw -> uw.getUser().getKeycloakId().equals(currentOwnerKeycloakId))
                        .map(uw -> uw.getUser().getId())
                        .findFirst()
                        .orElseThrow(() -> new AccessDeniedException("Not a member")), workspace.getId())
        ).orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        if (oldOwnerMembership.getRole() != WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Only the current OWNER can transfer workspace ownership.");
        }

        UserWorkspace newOwnerMembership = userWorkspaceRepository.findById(new UserWorkspaceId(newOwnerUserId, workspace.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a member of this workspace."));

        newOwnerMembership.setRole(WorkspaceRole.OWNER);
        oldOwnerMembership.setRole(WorkspaceRole.ADMIN);
        userWorkspaceRepository.save(newOwnerMembership);
        userWorkspaceRepository.save(oldOwnerMembership);
        log.info("Ownership of workspace [{}] transferred from [{}] to [{}]", tenantId, currentOwnerKeycloakId, newOwnerUserId);
    }

    private AppUser syncUserFromJwt(Jwt jwt) {
        AppUser newUser = new AppUser();
        newUser.setKeycloakId(jwt.getSubject());
        newUser.setUsername(jwt.getClaimAsString("preferred_username"));
        newUser.setEmail(jwt.getClaimAsString("username"));
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

    private Workspace findWorkspaceByTenantIdOrThrow(String tenantId) {
        return workspaceRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with tenant id: " + tenantId));
    }

    private UserWorkspace findUserWorkspaceByIdOrThrow(UserWorkspaceId id) {
        return userWorkspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for user in this workspace."));
    }
}
