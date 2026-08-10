package dev.eyadsharkawy.agency_os_api.tenant.project.dto;

import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "04.2. ProjectResponse", description = "Response details of a project")
public record ProjectResponse(
        @Schema(description = "Unique identifier of the project", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Name of the project", example = "Website Redesign")
        String name,

        @Schema(description = "Total budget allocated for the project", example = "15000.00")
        BigDecimal budget,

        @Schema(description = "Current status of the project", example = "IN_PROGRESS")
        ProjectStatus status,

        @Schema(description = "ID of the client associated with the project", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID clientId,

        @Schema(description = "Hourly billing rate for the project", example = "150.00")
        BigDecimal billingRate,

        @Schema(description = "Timestamp when the project was created", example = "2026-01-01T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when the project was last updated", example = "2026-01-02T12:00:00Z")
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
