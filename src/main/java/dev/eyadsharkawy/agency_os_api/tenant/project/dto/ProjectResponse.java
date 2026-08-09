package dev.eyadsharkawy.agency_os_api.tenant.project.dto;

import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        BigDecimal budget,
        ProjectStatus status,
        UUID clientId,
        BigDecimal billingRate,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getBudget(),
                project.getStatus(),
                project.getClient().getId(),
                project.getBillingRate(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
