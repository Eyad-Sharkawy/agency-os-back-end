package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceInvitation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
    name = "02.2. WorkspaceInvitationResponse",
    description = "Response details of a workspace invitation")
public record WorkspaceInvitationResponse(
    @Schema(
            description = "Unique identifier of the invitation",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,
    @Schema(
            description = "ID of the target workspace",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID workspaceId,
    @Schema(description = "Name of the target workspace", example = "Acme Workspace")
        String workspaceName,
    @Schema(description = "Username of the invited user", example = "jane_doe") String username,
    @Schema(description = "Username of the user who issued the invitation", example = "john_admin")
        String invitedByUsername,
    @Schema(description = "Assigned workspace role", example = "MEMBER") String role,
    @Schema(
            description = "Optional client ID bound to this invitation",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID clientId,
    @Schema(description = "Current status of the invitation", example = "PENDING") String status,
    @Schema(
            description = "Timestamp when the invitation was created",
            example = "2026-01-01T10:00:00Z")
        Instant createdAt) {
  public static WorkspaceInvitationResponse fromEntity(WorkspaceInvitation invitation) {
    return new WorkspaceInvitationResponse(
        invitation.getId(),
        invitation.getWorkspace().getId(),
        invitation.getWorkspace().getName(),
        invitation.getUsername(),
        invitation.getInvitedByUsername(),
        invitation.getRole().name(),
        invitation.getClientId(),
        invitation.getStatus().name(),
        invitation.getCreatedAt());
  }
}
