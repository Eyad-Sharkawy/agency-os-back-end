package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String tenantId,
        String contactEmail,
        String role,
        boolean isActive,
        Instant createdAt,
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