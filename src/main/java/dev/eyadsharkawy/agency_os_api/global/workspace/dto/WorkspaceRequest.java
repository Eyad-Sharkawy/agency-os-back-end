package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record WorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        String name,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Contact email must be a valid email address")
        String contactEmail
) {
}
