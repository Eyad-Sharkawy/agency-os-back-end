package dev.eyadsharkawy.agency_os_api.global.workspace.controller;

import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.WorkspaceInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(
    name = "02. Workspace Invitations",
    description =
        "Endpoints for sending, listing, accepting, and declining invitations to join workspaces")
public class WorkspaceInvitationController {

  private final WorkspaceInvitationService invitationService;

  @PostMapping("/{tenantId}/invitations")
  @PreAuthorize("@workspaceSecurity.hasRoleInTenant(#tenantId, 'OWNER', 'ADMIN')")
  @Operation(
      summary = "Send workspace invitation",
      description =
          "Allows workspace Owners and Admins to invite a teammate to join a workspace using their username or email. Only Owners can invite non-Member roles (e.g. Clients).")
  public ResponseEntity<WorkspaceInvitationResponse> inviteUser(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "The target workspace tenant ID") @PathVariable String tenantId,
      @Valid @RequestBody WorkspaceInvitationRequest request) {

    WorkspaceInvitationResponse response = invitationService.inviteUser(jwt, tenantId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/invitations")
  @Operation(
      summary = "List pending invitations",
      description =
          "Retrieves all pending workspace invitations sent to the authenticated user's email or username.")
  public ResponseEntity<List<WorkspaceInvitationResponse>> getPendingInvitations(
      @AuthenticationPrincipal Jwt jwt) {

    List<WorkspaceInvitationResponse> response = invitationService.getPendingInvitations(jwt);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/invitations/{invitationId}/accept")
  @Operation(
      summary = "Accept workspace invitation",
      description =
          "Accepts a pending invitation and automatically adds the user to the workspace. If the invitation is for a CLIENT role, they are also associated with the client company inside the tenant schema.")
  public ResponseEntity<Void> acceptInvitation(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "The unique ID of the invitation") @PathVariable UUID invitationId) {

    invitationService.acceptInvitation(jwt, invitationId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/invitations/{invitationId}/decline")
  @Operation(
      summary = "Decline workspace invitation",
      description = "Declines a pending workspace invitation, changing its status to DECLINED.")
  public ResponseEntity<Void> declineInvitation(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "The unique ID of the invitation") @PathVariable UUID invitationId) {

    invitationService.declineInvitation(jwt, invitationId);
    return ResponseEntity.ok().build();
  }
}
