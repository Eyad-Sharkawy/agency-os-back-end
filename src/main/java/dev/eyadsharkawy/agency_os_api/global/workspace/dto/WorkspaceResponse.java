package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String tenantId,
        String contactEmail,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkspaceResponse fromEntity(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getTenantId(),
                workspace.getContactEmail(),
                workspace.isActive(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}