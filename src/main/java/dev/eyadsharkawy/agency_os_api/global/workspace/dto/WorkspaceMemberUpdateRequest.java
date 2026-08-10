package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "01.4. WorkspaceMemberUpdateRequest", description = "Request payload for updating a workspace member's role or assigned client")
public record WorkspaceMemberUpdateRequest(
        @Schema(description = "New workspace role to assign to the member", example = "MEMBER")
        @NotNull(message = "Role is required")
        WorkspaceRole role,

        @Schema(description = "Optional client ID if restricted to a specific client", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID clientId
) {
}
