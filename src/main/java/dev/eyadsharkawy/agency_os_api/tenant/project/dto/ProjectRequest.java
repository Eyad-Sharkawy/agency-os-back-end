package dev.eyadsharkawy.agency_os_api.tenant.project.dto;

import dev.eyadsharkawy.agency_os_api.tenant.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ProjectRequest(
        @NotBlank(message = "Project name is required")
        String name,

        @PositiveOrZero(message = "Budget must be zero or positive")
        BigDecimal budget,

        @NotNull(message = "Project status is required")
        ProjectStatus status,

        @NotNull(message = "Client ID is required")
        UUID clientId
) {
}
