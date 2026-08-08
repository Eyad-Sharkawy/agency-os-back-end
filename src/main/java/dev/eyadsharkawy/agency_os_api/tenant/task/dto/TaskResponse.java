package dev.eyadsharkawy.agency_os_api.tenant.task.dto;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskPriority;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        Instant startDate,
        Instant dueDate,
        Integer estimatedMinutes,
        TaskPriority priority,
        TaskStatus status,
        UUID projectId,
        Set<String> assigneeIds,
        int totalLoggedMinutes,
        boolean isOverBudget,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse fromEntity(Task task, int totalLoggedMinutes) {
        boolean isOverBudget = task.getEstimatedMinutes() != null
                && task.getEstimatedMinutes() > 0
                && totalLoggedMinutes > task.getEstimatedMinutes();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStartDate(),
                task.getDueDate(),
                task.getEstimatedMinutes(),
                task.getPriority(),
                task.getStatus(),
                task.getProject().getId(),
                task.getAssigneeIds(),
                totalLoggedMinutes,
                isOverBudget,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
