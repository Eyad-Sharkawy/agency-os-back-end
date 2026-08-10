package dev.eyadsharkawy.agency_os_api.tenant.task.dto;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskPriority;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(name = "05.2. TaskResponse", description = "Response details of a task")
public record TaskResponse(
    @Schema(
            description = "Unique identifier of the task",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,
    @Schema(description = "Title of the task", example = "Design Homepage Wireframe") String title,
    @Schema(
            description = "Detailed description of the task",
            example = "Create high-fidelity wireframes for desktop and mobile layouts")
        String description,
    @Schema(description = "Start timestamp of the task", example = "2026-02-01T09:00:00Z")
        Instant startDate,
    @Schema(description = "Due timestamp of the task", example = "2026-02-15T18:00:00Z")
        Instant dueDate,
    @Schema(description = "Estimated minutes for completing the task", example = "480")
        Integer estimatedMinutes,
    @Schema(description = "Priority level of the task", example = "HIGH") TaskPriority priority,
    @Schema(description = "Current status of the task", example = "IN_PROGRESS") TaskStatus status,
    @Schema(
            description = "ID of the associated project",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID projectId,
    @Schema(
            description = "Set of user IDs assigned to this task",
            example = "[\"user-123\", \"user-456\"]")
        Set<String> assigneeIds,
    @Schema(description = "Total time logged against this task in minutes", example = "300")
        int totalLoggedMinutes,
    @Schema(description = "Whether logged time exceeds estimated time", example = "false")
        boolean isOverBudget,
    @Schema(description = "Timestamp when the task was created", example = "2026-01-01T10:00:00Z")
        Instant createdAt,
    @Schema(
            description = "Timestamp when the task was last updated",
            example = "2026-01-02T12:00:00Z")
        Instant updatedAt) {
  public static TaskResponse fromEntity(Task task, int totalLoggedMinutes) {
    boolean isOverBudget =
        task.getEstimatedMinutes() != null
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
        Set.copyOf(task.getAssigneeIds()),
        totalLoggedMinutes,
        isOverBudget,
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
