package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        String name
) {
}
