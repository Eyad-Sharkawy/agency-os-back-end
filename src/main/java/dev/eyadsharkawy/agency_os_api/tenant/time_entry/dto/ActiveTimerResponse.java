package dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto;

import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.ActiveTimer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "06.3. ActiveTimerResponse", description = "Response details of an active running timer")
public record ActiveTimerResponse(
        @Schema(description = "ID of the user running the timer", example = "user-123")
        String userId,

        @Schema(description = "ID of the task currently being timed", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID taskId,

        @Schema(description = "Timestamp when the timer was started", example = "2026-02-01T10:00:00Z")
        Instant startTime
) {
    public static ActiveTimerResponse fromEntity(ActiveTimer timer) {
        return new ActiveTimerResponse(
                timer.getUserId(),
                timer.getTask().getId(),
                timer.getStartTime()
        );
    }
}
