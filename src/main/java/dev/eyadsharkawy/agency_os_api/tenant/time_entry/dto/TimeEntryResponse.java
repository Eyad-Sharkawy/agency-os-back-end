package dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto;

import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "06.2. TimeEntryResponse", description = "Response details of a logged time entry")
public record TimeEntryResponse(
    @Schema(
            description = "Unique identifier of the time entry",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,
    @Schema(
            description = "ID of the task associated with this time entry",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID taskId,
    @Schema(description = "ID of the user who logged the time entry", example = "user-123")
        String userId,
    @Schema(description = "Duration of logged time in minutes", example = "60") int durationMinutes,
    @Schema(description = "Whether this time entry is billable to the client", example = "true")
        boolean isBillable,
    @Schema(
            description = "Timestamp when the time entry was logged",
            example = "2026-01-01T10:00:00Z")
        Instant createdAt,
    @Schema(
            description = "Timestamp when the time entry was last updated",
            example = "2026-01-02T12:00:00Z")
        Instant updatedAt) {
  public static TimeEntryResponse fromEntity(TimeEntry entry) {
    return new TimeEntryResponse(
        entry.getId(),
        entry.getTask().getId(),
        entry.getUserId(),
        entry.getDurationMinutes(),
        entry.isBillable(),
        entry.getCreatedAt(),
        entry.getUpdatedAt());
  }
}
