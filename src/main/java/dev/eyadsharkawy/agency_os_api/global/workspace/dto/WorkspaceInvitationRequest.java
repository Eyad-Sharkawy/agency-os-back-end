package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "02.1. WorkspaceInvitationRequest", description = "Request payload to send an invitation to join a workspace")
public record WorkspaceInvitationRequest(
        @Schema(description = "Email address of the user being invited", example = "invitee@example.com")
        String email,

        @Schema(description = "Username of the user being invited", example = "jane_doe")
        String username,

        @Schema(description = "Workspace role to be assigned upon joining", example = "MEMBER")
        @NotNull(message = "Role is required")
        WorkspaceRole role,

        @Schema(description = "Optional client ID to restrict access if role is CLIENT", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID clientId
) {
}
