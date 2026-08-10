package dev.eyadsharkawy.agency_os_api.tenant.task.dto;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    name = "05.3. TaskStatusUpdateRequest",
    description = "Request payload for updating the status of a task")
public record TaskStatusUpdateRequest(
    @Schema(description = "New status to apply to the task", example = "COMPLETED")
        @NotNull(message = "Status is required")
        TaskStatus status) {}
