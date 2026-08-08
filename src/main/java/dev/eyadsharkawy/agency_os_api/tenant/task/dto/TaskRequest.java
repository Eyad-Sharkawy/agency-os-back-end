package dev.eyadsharkawy.agency_os_api.tenant.task.dto;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskPriority;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TaskRequest(
        @NotBlank(message = "Task title is required")
        String title,

        String description,

        Instant startDate,

        Instant dueDate,

        @PositiveOrZero(message = "Estimation must be zero or higher")
        Integer estimatedMinutes,

        @NotNull(message = "Task priority is required")
        TaskPriority priority,

        @NotNull(message = "Task status is required")
        TaskStatus status,

        @NotNull(message = "Project ID is required")
        UUID projectId,

        @NotNull(message = "Assignee set id required")
        Set<String> assigneeIds

) {
}
