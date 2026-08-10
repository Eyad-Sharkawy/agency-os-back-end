package dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(name = "06.1. TimeEntryRequest", description = "Request payload for creating a time entry")
public record TimeEntryRequest(
        @Schema(description = "ID of the task associated with this time entry", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "Task ID is required")
        UUID taskId,

        @Schema(description = "Duration of logged time in minutes", example = "60")
        @Positive(message = "Duration must be greater than zero")
        int durationMinutes,

        @Schema(description = "Whether this time entry is billable to the client", example = "true")
        boolean isBillable
) {
}
