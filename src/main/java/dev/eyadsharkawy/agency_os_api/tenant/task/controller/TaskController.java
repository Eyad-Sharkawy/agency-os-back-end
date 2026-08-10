package dev.eyadsharkawy.agency_os_api.tenant.task.controller;

import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskResponse;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskStatusUpdateRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(
    name = "05. Tasks",
    description =
        "Endpoints for task backlog planning, assignment tracking, and status/progress updates")
public class TaskController {

  private final TaskService taskService;

  @PostMapping
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
  @Operation(
      summary = "Create task",
      description = "Creates a new task within a project. Restricted to OWNER or ADMIN.")
  public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
    TaskResponse response = taskService.createTask(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')")
  @Operation(
      summary = "List all tasks",
      description =
          "Retrieves all tasks in the active tenant. MEMBER users only see tasks assigned directly to them. Restricted to OWNER, ADMIN, MEMBER, or CLIENT.")
  public ResponseEntity<List<TaskResponse>> getAllTasks() {
    List<TaskResponse> responses = taskService.getAllTasks();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{id}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')")
  @Operation(
      summary = "Get task by ID",
      description =
          "Retrieves details of a specific task. Verifies that MEMBER users are assigned to this task before returning. Restricted to OWNER, ADMIN, MEMBER, or CLIENT.")
  public ResponseEntity<TaskResponse> getTaskById(
      @Parameter(description = "The task unique ID") @PathVariable UUID id) {
    TaskResponse response = taskService.getTaskById(id);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/project/{projectId}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')")
  @Operation(
      summary = "Get tasks by Project",
      description =
          "Lists all tasks for a specific project. MEMBER users are filtered to only see tasks they are assigned to. Restricted to OWNER, ADMIN, MEMBER, or CLIENT.")
  public ResponseEntity<List<TaskResponse>> getTasksByProjectId(
      @Parameter(description = "The project unique ID") @PathVariable UUID projectId) {
    List<TaskResponse> responses = taskService.getTasksByProjectId(projectId);
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/assignee/{assigneeId}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER')")
  @Operation(
      summary = "Get tasks by Assignee",
      description =
          "Lists tasks assigned to a specific user. MEMBER users are blocked from querying assignments of other users. Restricted to OWNER, ADMIN, or MEMBER.")
  public ResponseEntity<List<TaskResponse>> getTasksByAssigneeId(
      @Parameter(description = "The assignee's Keycloak ID") @PathVariable String assigneeId) {
    List<TaskResponse> responses = taskService.getTasksByAssigneeId(assigneeId);
    return ResponseEntity.ok(responses);
  }

  @PutMapping("/{id}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
  @Operation(
      summary = "Update task details",
      description =
          "Performs a full update on task details (title, description, dates, assignees). Restricted to OWNER or ADMIN.")
  public ResponseEntity<TaskResponse> updateTask(
      @Parameter(description = "The task unique ID") @PathVariable UUID id,
      @Valid @RequestBody TaskRequest request) {
    TaskResponse response = taskService.updateTaskById(id, request);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER')")
  @Operation(
      summary = "Update task progress status",
      description =
          "Allows internal users to update only the status (progress) of a task. MEMBER users can only call this for tasks they are assigned to.")
  public ResponseEntity<TaskResponse> updateTaskStatus(
      @Parameter(description = "The task unique ID") @PathVariable UUID id,
      @Valid @RequestBody TaskStatusUpdateRequest request) {
    TaskResponse response = taskService.updateTaskStatus(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
  @Operation(
      summary = "Delete task",
      description = "Permanently deletes a task. Restricted to OWNER or ADMIN.")
  public ResponseEntity<Void> deleteTask(
      @Parameter(description = "The task unique ID") @PathVariable UUID id) {
    taskService.deleteTaskById(id);
    return ResponseEntity.noContent().build();
  }
}
