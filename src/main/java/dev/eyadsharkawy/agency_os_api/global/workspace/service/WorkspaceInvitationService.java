package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.user.service.UserSyncService;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.*;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceInvitationRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
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
  private final UserSyncService userSyncService;
  private final UserWorkspaceRepository userWorkspaceRepository;
  private final ClientUserRegistrationService clientUserRegistrationService;

  @Transactional
  public WorkspaceInvitationResponse inviteUser(
      Jwt inviterJwt, String tenantId, WorkspaceInvitationRequest request) {
    Workspace workspace =
        workspaceRepository
            .findByTenantId(tenantId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Workspace not found with Id: " + tenantId));

    AppUser invitee = resolveInvitee(request);
    validateInviterPermissions(inviterJwt, tenantId, request.role());
    validateExistingMembership(
        invitee.getKeycloakId(), tenantId, request.role(), request.clientId());

    Optional<WorkspaceInvitation> existingInvitation =
        findAndValidatePendingInvitation(workspace.getId(), invitee.getUsername());

    String inviterUsername = inviterJwt.getClaimAsString("preferred_username");
    if (inviterUsername == null) {
      inviterUsername = "Anonymous Admin";
    }

    WorkspaceInvitation invitation = existingInvitation.orElseGet(WorkspaceInvitation::new);
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

  private AppUser resolveInvitee(WorkspaceInvitationRequest request) {
    if (request.username() != null && !request.username().isBlank()) {
      return userRepository
          .findByUsernameIgnoreCase(request.username().trim())
          .orElseThrow(
              () ->
                  new ResourceNotFoundException(
                      "User not found with username: " + request.username().trim()));
    }
    if (request.email() != null && !request.email().isBlank()) {
      return userRepository
          .findByEmailIgnoreCase(request.email().trim())
          .orElseThrow(
              () ->
                  new ResourceNotFoundException(
                      "User not found with email: " + request.email().trim()));
    }
    throw new IllegalArgumentException(
        "Either username or email must be provided to invite a user.");
  }

  private void validateInviterPermissions(
      Jwt inviterJwt, String tenantId, WorkspaceRole targetRole) {
    if (targetRole == WorkspaceRole.MEMBER) {
      return;
    }
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

  private void validateExistingMembership(
      String keycloakId, String tenantId, WorkspaceRole targetRole, UUID clientId) {
    Optional<WorkspaceRole> existingRoleOpt =
        userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);

    if (existingRoleOpt.isEmpty()) {
      return;
    }

    WorkspaceRole existingRole = existingRoleOpt.get();
    if (existingRole == WorkspaceRole.OWNER) {
      throw new IllegalArgumentException("The workspace owner cannot be converted or invited.");
    }

    if (targetRole == WorkspaceRole.CLIENT) {
      if (clientId == null) {
        throw new IllegalArgumentException("Client ID is required for client invitations.");
      }
      return;
    }

    if (existingRole == WorkspaceRole.CLIENT) {
      throw new IllegalArgumentException(
          "This user is currently registered as an external client user. They cannot be invited as a team member.");
    }
    throw new IllegalArgumentException("User is already an active team member of this workspace.");
  }

  private Optional<WorkspaceInvitation> findAndValidatePendingInvitation(
      UUID workspaceId, String username) {
    Optional<WorkspaceInvitation> existingInvitation =
        invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(workspaceId, username);

    if (existingInvitation.isPresent()
        && existingInvitation.get().getStatus() == InvitationStatus.PENDING) {
      throw new IllegalArgumentException("An invitation is already pending for this user.");
    }
    return existingInvitation;
  }

  @Transactional(readOnly = true)
  public List<WorkspaceInvitationResponse> getPendingInvitations(Jwt jwt) {
    AppUser user = userSyncService.getOrSyncUser(jwt);
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

    AppUser user = userSyncService.getOrSyncUser(jwt);

    if (!user.getUsername().equalsIgnoreCase(invitation.getUsername())) {
      throw new IllegalArgumentException("You are not authorized to accept this invitation.");
    }

    UserWorkspace membership =
        userWorkspaceRepository
            .findById(new UserWorkspaceId(user.getId(), invitation.getWorkspace().getId()))
            .orElseGet(
                () -> {
                  UserWorkspace uw = new UserWorkspace();
                  uw.setUser(user);
                  uw.setWorkspace(invitation.getWorkspace());
                  uw.getId().setUserId(user.getId());
                  uw.getId().setWorkspaceId(invitation.getWorkspace().getId());
                  return uw;
                });
    membership.setRole(invitation.getRole());

    if (invitation.getRole() == WorkspaceRole.CLIENT) {
      registerClientUserAccess(invitation, user.getKeycloakId());
    }

    invitation.setStatus(InvitationStatus.ACCEPTED);
    invitationRepository.save(invitation);
    userWorkspaceRepository.save(membership);

    log.info(
        "User [{}] accepted invitation for workspace [{}]",
        user.getUsername(),
        invitation.getWorkspace().getTenantId());
  }

  private void registerClientUserAccess(WorkspaceInvitation invitation, String keycloakId) {
    if (invitation.getClientId() == null) {
      throw new IllegalArgumentException("Client ID is required for client invitation.");
    }

    String tenantId = invitation.getWorkspace().getTenantId();
    TenantContextHolder.setTenantId(tenantId);
    try {
      clientUserRegistrationService.registerClientUser(keycloakId, invitation.getClientId());
    } finally {
      TenantContextHolder.clear();
    }
  }

  @Transactional
  public void declineInvitation(Jwt jwt, UUID invitationId) {
    WorkspaceInvitation invitation =
        invitationRepository
            .findById(invitationId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Invitation not found with id: " + invitationId));

    AppUser user = userSyncService.getOrSyncUser(jwt);

    if (!user.getUsername().equalsIgnoreCase(invitation.getUsername())) {
      throw new IllegalArgumentException("You are not authorized to accept this invitation");
    }

    invitation.setStatus(InvitationStatus.DECLINED);
    invitationRepository.save(invitation);

    log.info(
        "User [{}] declined invitation for workspace [{}]",
        user.getUsername(),
        invitation.getWorkspace().getTenantId());
  }
}
