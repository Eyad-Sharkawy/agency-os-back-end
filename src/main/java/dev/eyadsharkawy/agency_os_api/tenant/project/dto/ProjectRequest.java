package dev.eyadsharkawy.agency_os_api.tenant.project.dto;

import dev.eyadsharkawy.agency_os_api.tenant.project.entity.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "04.1. ProjectRequest", description = "Request payload for creating or updating a project")
public record ProjectRequest(
        @Schema(description = "Name of the project", example = "Website Redesign")
        @NotBlank(message = "Project name is required")
        String name,

        @Schema(description = "Project budget amount", example = "15000.00")
        @PositiveOrZero(message = "Budget must be zero or positive")
        BigDecimal budget,

        @Schema(description = "Status of the project", example = "IN_PROGRESS")
        @NotNull(message = "Project status is required")
        ProjectStatus status,

        @Schema(description = "Associated client ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "Client ID is required")
        UUID clientId,

        @Schema(description = "Hourly billing rate for the project", example = "150.00")
        @NotNull(message = "Billing rate is required")
        @PositiveOrZero(message = "Billing rate must be zero or positive")
        BigDecimal billingRate
) {
}
