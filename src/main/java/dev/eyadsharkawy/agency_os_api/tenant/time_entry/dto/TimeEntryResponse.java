package dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto;

import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;

import java.time.Instant;
import java.util.UUID;

public record TimeEntryResponse(
        UUID id,
        UUID taskId,
        String userId,
        int durationMinutes,
        boolean isBillable,
        Instant createdAt,
        Instant updatedAt
) {
    public static TimeEntryResponse fromEntity(TimeEntry entry) {
        return new TimeEntryResponse(
                entry.getId(),
                entry.getTask().getId(),
                entry.getUserId(),
                entry.getDurationMinutes(),
                entry.isBillable(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
