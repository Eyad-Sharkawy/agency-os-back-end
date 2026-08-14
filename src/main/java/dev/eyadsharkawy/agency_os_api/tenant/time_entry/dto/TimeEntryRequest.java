package dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

@Schema(name = "06.1. TimeEntryRequest", description = "Request payload for creating a time entry")
public record TimeEntryRequest(
    @Schema(
            description = "ID of the task associated with this time entry",
            example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "Task ID is required")
        UUID taskId,
    @Schema(description = "Duration of logged time in minutes", example = "60")
        @Positive(message = "Duration must be greater than zero")
        int durationMinutes,
    @Schema(description = "Whether this time entry is billable to the client", example = "true")
        boolean isBillable,
    @Schema(
            description =
                "Keycloak user ID on whose behalf the time is logged (Optional: OWNER and ADMIN only, defaults to authenticated user)",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String userId) {

  public TimeEntryRequest(UUID taskId, int durationMinutes, boolean isBillable) {
    this(taskId, durationMinutes, isBillable, null);
  }
}
