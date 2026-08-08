package dev.eyadsharkawy.agency_os_api.tenant.task.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.project.repository.ProjectRepository;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskResponse;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        log.info("Creating task [{}] for project [{}]", request.title(), request.projectId());

        Project project = findProjectByIdOrThrow(request.projectId());
        Task task = new Task();
        task.mapFromRequestWithProject(request, project);


        Task savedTask = taskRepository.save(task);

        return TaskResponse.fromEntity(savedTask, 0);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        log.info("Fetching all tasks");
        return taskRepository.findAll().stream()
                .map(task -> TaskResponse.fromEntity(task, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID id) {
        log.info("Fetching task with id: {}", id);
        Task task = findTaskByIdOrThrow(id);

        return TaskResponse.fromEntity(task, 0);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProjectId(UUID projectId) {
        log.info("Fetching tasks for project: {}", projectId);
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        return taskRepository.findByProjectId(projectId).stream()
                .map(task -> TaskResponse.fromEntity(task, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByAssigneeId(String assigneeId) {
        log.info("Fetching tasks for assignee: {}", assigneeId);

        return taskRepository.findByAssigneeId(assigneeId).stream()
                .map(task -> TaskResponse.fromEntity(task, 0))
                .toList();
    }

    @Transactional
    public TaskResponse updateTaskById(UUID id, TaskRequest request) {
        log.info("Updating task with id: {}", id);
        Task task = findTaskByIdOrThrow(id);

        Project project = findProjectByIdOrThrow(request.projectId());

        task.mapFromRequestWithProject(request, project);

        Task updatedTask = taskRepository.save(task);

        return TaskResponse.fromEntity(updatedTask, 0);
    }

    @Transactional
    public void deleteTaskById(UUID id) {
        log.info("Deleting task with id: {}", id);
        Task task = findTaskByIdOrThrow(id);
        taskRepository.delete(task);
    }

    private Task findTaskByIdOrThrow(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private Project findProjectByIdOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }
}
