package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(
    name = "01.1. WorkspaceRequest",
    description = "Request payload for creating or updating a workspace")
public record WorkspaceRequest(
    @Schema(description = "Name of the workspace", example = "Acme Agency")
        @NotBlank(message = "Workspace name is required")
        String name) {}
