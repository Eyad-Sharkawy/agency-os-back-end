package dev.eyadsharkawy.agency_os_api.tenant.task.dto;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskPriority;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(name = "05.1. TaskRequest", description = "Request payload for creating or updating a task")
public record TaskRequest(
    @Schema(description = "Title of the task", example = "Design Homepage Wireframe")
        @NotBlank(message = "Task title is required")
        String title,
    @Schema(
            description = "Detailed description of the task",
            example = "Create high-fidelity wireframes for desktop and mobile layouts")
        String description,
    @Schema(description = "Task start timestamp", example = "2026-02-01T09:00:00Z")
        Instant startDate,
    @Schema(description = "Task due timestamp", example = "2026-02-15T18:00:00Z") Instant dueDate,
    @Schema(description = "Estimated time to complete the task in minutes", example = "480")
        @PositiveOrZero(message = "Estimation must be zero or higher")
        Integer estimatedMinutes,
    @Schema(description = "Priority level of the task", example = "HIGH")
        @NotNull(message = "Task priority is required")
        TaskPriority priority,
    @Schema(description = "Current status of the task", example = "IN_PROGRESS")
        @NotNull(message = "Task status is required")
        TaskStatus status,
    @Schema(
            description = "ID of the project this task belongs to",
            example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "Project ID is required")
        UUID projectId,
    @Schema(
            description = "Set of user IDs assigned to this task",
            example = "[\"user-123\", \"user-456\"]")
        @NotNull(message = "Assignee set id required")
        Set<String> assigneeIds) {}
