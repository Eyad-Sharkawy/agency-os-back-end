package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceInvitation;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceInvitationResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        String username,
        String invitedByUsername,
        String role,
        UUID clientId,
        String status,
        Instant createdAt
) {
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
                invitation.getCreatedAt()
        );
    }
}
