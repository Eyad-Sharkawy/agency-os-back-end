package dev.eyadsharkawy.agency_os_api.tenant.task.dto;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(
        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
