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

    AppUser invitee;

    if (request.username() != null && !request.username().isBlank())
      invitee =
          userRepository
              .findByUsernameIgnoreCase(request.username().trim())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "User not found with username: " + request.username().trim()));
    else if (request.email() != null && !request.email().isBlank())
      invitee =
          userRepository
              .findByEmailIgnoreCase(request.email().trim())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "User not found with email: " + request.email().trim()));
    else
      throw new IllegalArgumentException(
          "Either username or email must be provided to invite a user.");

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

    Optional<WorkspaceRole> existingRoleOpt =
        userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(invitee.getKeycloakId(), tenantId);

    if (existingRoleOpt.isPresent()) {
      WorkspaceRole existingRole = existingRoleOpt.get();
      if (existingRole == WorkspaceRole.OWNER) {
        throw new IllegalArgumentException("The workspace owner cannot be converted or invited.");
      }

      if (request.role() == WorkspaceRole.CLIENT) {
        if (request.clientId() == null) {
          throw new IllegalArgumentException("Client ID is required for client invitations.");
        }
        // Inviting as CLIENT by the OWNER allows converting an existing team member to CLIENT
      } else {
        // Inviting as internal team member (MEMBER or ADMIN) via general workspace invite
        if (existingRole == WorkspaceRole.CLIENT) {
          throw new IllegalArgumentException(
              "This user is currently registered as an external client user. They cannot be invited as a team member.");
        } else {
          throw new IllegalArgumentException(
              "User is already an active team member of this workspace.");
        }
      }
    }

    Optional<WorkspaceInvitation> existingInvitation =
        invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(
            workspace.getId(), invitee.getUsername());

    if (existingInvitation.isPresent()
        && existingInvitation.get().getStatus() == InvitationStatus.PENDING) {
      throw new IllegalArgumentException("An invitation is already pending for this user.");
    }

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

    if (!user.getUsername().equalsIgnoreCase(invitation.getUsername()))
      throw new IllegalArgumentException("You are not authorized to accept this invitation.");

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
      if (invitation.getClientId() == null)
        throw new IllegalArgumentException("Client ID is required for client invitation.");

      String tenantId = invitation.getWorkspace().getTenantId();
      TenantContextHolder.setTenantId(tenantId);
      try {
        clientUserRegistrationService.registerClientUser(
            user.getKeycloakId(), invitation.getClientId());
      } finally {
        TenantContextHolder.clear();
      }
    }

    invitation.setStatus(InvitationStatus.ACCEPTED);
    invitationRepository.save(invitation);

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

    AppUser user = userSyncService.getOrSyncUser(jwt);

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
