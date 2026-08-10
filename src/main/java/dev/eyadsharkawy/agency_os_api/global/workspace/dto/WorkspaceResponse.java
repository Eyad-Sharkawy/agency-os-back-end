package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "01.2. WorkspaceResponse", description = "Response details of a workspace")
public record WorkspaceResponse(
        @Schema(description = "Unique identifier of the workspace", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Name of the workspace", example = "Acme Agency")
        String name,

        @Schema(description = "Tenant identifier associated with the workspace", example = "tenant-acme")
        String tenantId,

        @Schema(description = "Contact email of the workspace owner/admin", example = "admin@acme.com")
        String contactEmail,

        @Schema(description = "User's role in the workspace", example = "OWNER")
        String role,

        @Schema(description = "Whether the workspace is active", example = "true")
        boolean isActive,

        @Schema(description = "Timestamp when the workspace was created", example = "2026-01-01T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when the workspace was last updated", example = "2026-01-02T12:00:00Z")
        Instant updatedAt
) {
    public static WorkspaceResponse fromEntity(Workspace workspace, WorkspaceRole role) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getTenantId(),
                workspace.getContactEmail(),
                role.name(),
                workspace.isActive(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    public static WorkspaceResponse fromEntity(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getTenantId(),
                workspace.getContactEmail(),
                null,
                workspace.isActive(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}