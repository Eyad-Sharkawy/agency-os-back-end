package dev.eyadsharkawy.agency_os_api.tenant.task.controller;

import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskResponse;
import dev.eyadsharkawy.agency_os_api.tenant.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<TaskResponse> responses = taskService.getAllTasks();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable UUID id) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getTasksByProjectId(@PathVariable UUID projectId) {
        List<TaskResponse> responses = taskService.getTasksByProjectId(projectId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<List<TaskResponse>> getTasksByAssigneeId(@PathVariable String assigneeId) {
        List<TaskResponse> responses = taskService.getTasksByAssigneeId(assigneeId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.updateTaskById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taskService.deleteTaskById(id);
        return ResponseEntity.noContent().build();
    }
}
