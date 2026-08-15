package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.*;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceInvitationRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceInvitationService {
  private final WorkspaceInvitationRepository invitationRepository;
  private final WorkspaceRepository workspaceRepository;
  private final AppUserRepository userRepository;
  private final UserWorkspaceRepository userWorkspaceRepository;
  private final ClientUserRepository clientUserRepository;
  private final ClientRepository clientRepository;

  @Transactional
  public WorkspaceInvitationResponse inviteUser(
      Jwt inviterJwt, String tenantId, WorkspaceInvitationRequest request) {
    Workspace workspace =
        workspaceRepository
            .findByTenantId(tenantId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Workspace not found with Id: " + tenantId));

    AppUser invitee;

    if (request.username() != null && !request.username().isBlank())
      invitee =
          userRepository
              .findByUsername(request.username())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "User not found with username: " + request.username()));
    else if (request.email() != null && !request.email().isBlank())
      invitee =
          userRepository
              .findByEmail(request.email())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "User not found with username: " + request.email()));
    else
      throw new IllegalArgumentException(
          "Either username or username must be provided to invite a user.");

    if (request.role() != WorkspaceRole.MEMBER) {
      String inviterKeycloakId = inviterJwt.getSubject();
      WorkspaceRole inviterRole =
          userWorkspaceRepository
              .findRoleByKeycloakIdAndTenantId(inviterKeycloakId, tenantId)
              .orElseThrow(
                  () ->
                      new AccessDeniedException(
                          "Access Denied: Inviter is not a member of this workspace."));

      if (inviterRole != WorkspaceRole.OWNER) {
        throw new AccessDeniedException("Only workspace OWNER can invite non MEMBER users.");
      }
    }

    boolean isMember =
        userWorkspaceRepository
            .findRoleByKeycloakIdAndTenantId(invitee.getKeycloakId(), tenantId)
            .isPresent();

    if (isMember) throw new IllegalArgumentException("User is already a member of this workspace.");

    invitationRepository
        .findByWorkspaceIdAndUsernameIgnoreCase(workspace.getId(), invitee.getUsername())
        .filter(inv -> inv.getStatus() == InvitationStatus.PENDING)
        .ifPresent(
            inv -> {
              throw new IllegalArgumentException("An invitation is already pending for this user.");
            });

    String inviterUsername = inviterJwt.getClaimAsString("preferred_username");

    if (inviterUsername == null) {
      inviterUsername = "Anonymous Admin";
    }

    WorkspaceInvitation invitation = new WorkspaceInvitation();
    invitation.setWorkspace(workspace);
    invitation.setUsername(invitee.getUsername());
    invitation.setInvitedByUsername(inviterUsername);
    invitation.setRole(request.role());
    invitation.setClientId(request.clientId());
    invitation.setStatus(InvitationStatus.PENDING);

    WorkspaceInvitation savedInvitation = invitationRepository.save(invitation);
    log.info(
        "Invitation created for username [{}] in workspace [{}] by [{}]",
        invitee.getUsername(),
        tenantId,
        inviterUsername);

    return WorkspaceInvitationResponse.fromEntity(savedInvitation);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceInvitationResponse> getPendingInvitations(Jwt jwt) {
    String keycloakId = jwt.getSubject();
    AppUser user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    return invitationRepository
        .findByUsernameIgnoreCaseAndStatus(user.getUsername(), InvitationStatus.PENDING)
        .stream()
        .map(WorkspaceInvitationResponse::fromEntity)
        .toList();
  }

  @Transactional
  public void acceptInvitation(Jwt jwt, UUID invitationId) {
    WorkspaceInvitation invitation =
        invitationRepository
            .findById(invitationId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Invitation not found with id: " + invitationId));

    String keycloakId = jwt.getSubject();
    AppUser user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (!user.getUsername().equalsIgnoreCase(invitation.getUsername()))
      throw new IllegalArgumentException("You are not authorized to accept this invitation.");

    UserWorkspace membership = new UserWorkspace();
    membership.setUser(user);
    membership.setWorkspace(invitation.getWorkspace());
    membership.setRole(invitation.getRole());

    if (invitation.getRole() == WorkspaceRole.CLIENT) {
      if (invitation.getClientId() == null)
        throw new IllegalArgumentException("Client ID is required for client invitation.");

      String tenantId = invitation.getWorkspace().getTenantId();
      TenantContextHolder.setTenantId(tenantId);
      try {
        Client client =
            clientRepository
                .findById(invitation.getClientId())
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "Client not found with id: " + invitation.getClientId()));

        ClientUser clientUser = new ClientUser();
        clientUser.setUserId(user.getKeycloakId());
        clientUser.setClient(client);

        clientUserRepository.save(clientUser);
      } finally {
        TenantContextHolder.clear();
      }
    }

    invitation.setStatus(InvitationStatus.ACCEPTED);
    invitationRepository.save(invitation);

    // Manually set ID values in the composite key and save directly to prevent cascading issues
    membership.getId().setUserId(user.getId());
    membership.getId().setWorkspaceId(invitation.getWorkspace().getId());
    userWorkspaceRepository.save(membership);

    log.info(
        "User [{}] accepted invitation for workspace [{}]",
        user.getUsername(),
        invitation.getWorkspace().getTenantId());
  }

  @Transactional
  public void declineInvitation(Jwt jwt, UUID invitationId) {
    WorkspaceInvitation invitation =
        invitationRepository
            .findById(invitationId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Invitation not found with id: " + invitationId));

    String keycloakId = jwt.getSubject();
    AppUser user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if (!user.getUsername().equalsIgnoreCase(invitation.getUsername()))
      throw new IllegalArgumentException("You are not authorized to accept this invitation");

    invitation.setStatus(InvitationStatus.DECLINED);
    invitationRepository.save(invitation);
    log.info(
        "User [{}] declined invitation for workspace [{}]",
        user.getUsername(),
        invitation.getWorkspace().getTenantId());
  }
}
