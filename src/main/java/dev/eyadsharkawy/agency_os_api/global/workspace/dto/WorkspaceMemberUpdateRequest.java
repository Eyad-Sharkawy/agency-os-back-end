package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WorkspaceMemberUpdateRequest(
        @NotNull(message = "Role is required")
        WorkspaceRole role,
        UUID clientId
) {}
