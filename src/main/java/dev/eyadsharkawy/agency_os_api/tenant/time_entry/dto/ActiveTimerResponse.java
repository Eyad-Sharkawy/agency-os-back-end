package dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto;

import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.ActiveTimer;

import java.time.Instant;
import java.util.UUID;

public record ActiveTimerResponse(
        String userId,
        UUID taskId,
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
