package dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record TimeEntryRequest(
        @NotNull(message = "Task ID is required")
        UUID taskId,

        @Positive(message = "Duration must be greater than zero")
        int durationMinutes,

        boolean isBillable
) {
}
