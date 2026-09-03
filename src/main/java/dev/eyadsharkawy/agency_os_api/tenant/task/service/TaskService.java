package dev.eyadsharkawy.agency_os_api.tenant.task.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.ClientUserRegistrationService;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.project.repository.ProjectRepository;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskResponse;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskStatusUpdateRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.task.repository.TaskRepository;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository.TimeEntryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final TimeEntryRepository timeEntryRepository;
  private final UserWorkspaceRepository userWorkspaceRepository;
  private final ClientUserRegistrationService clientUserRegistrationService;

  @Transactional
  public TaskResponse createTask(TaskRequest request) {
    log.info("Creating task [{}] for project [{}]", request.title(), request.projectId());

    validateWriteAccess("create tasks");
    validateAssignees(request);
    Project project = findProjectByIdOrThrow(request.projectId());
    Task task = new Task();
    task.mapFromRequestWithProject(request, project);

    Task savedTask = taskRepository.save(task);
    return TaskResponse.fromEntity(savedTask, getTotalLoggedTimeById(savedTask.getId()));
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> getAllTasks() {
    log.info("Fetching tasks");

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()) {
        WorkspaceRole role = roleOpt.get();
        if (role == WorkspaceRole.MEMBER) {
          log.info("Member user [{}] queried tasks. Filtering by assigned tasks.", keycloakId);
          return taskRepository.findByAssigneeId(keycloakId).stream()
              .map(task -> TaskResponse.fromEntity(task, getTotalLoggedTimeById(task.getId())))
              .toList();
        } else if (role == WorkspaceRole.CLIENT) {
          log.info("Client user [{}] queried tasks. Filtering by client projects.", keycloakId);
          var clientIdOpt = clientUserRegistrationService.resolveClientId(keycloakId, tenantId);
          if (clientIdOpt.isPresent()) {
            return taskRepository.findByProjectClientId(clientIdOpt.get()).stream()
                .map(task -> TaskResponse.fromEntity(task, getTotalLoggedTimeById(task.getId())))
                .toList();
          }
          return List.of();
        }
      }
    }

    return taskRepository.findAll().stream()
        .map(task -> TaskResponse.fromEntity(task, getTotalLoggedTimeById(task.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public TaskResponse getTaskById(UUID id) {
    log.info("Fetching task with id: {}", id);
    Task task = findTaskByIdOrThrow(id);

    validateUserAccessToTask(task);

    return TaskResponse.fromEntity(task, getTotalLoggedTimeById(task.getId()));
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> getTasksByProjectId(UUID projectId) {
    log.info("Fetching tasks for project: {}", projectId);
    if (!projectRepository.existsById(projectId)) {
      throw new ResourceNotFoundException("Project not found with id: " + projectId);
    }

    List<Task> tasks = taskRepository.findByProjectId(projectId);

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()) {
        WorkspaceRole role = roleOpt.get();
        if (role == WorkspaceRole.MEMBER) {
          // Members can only see their assigned tasks within that project
          return tasks.stream()
              .filter(task -> task.getAssigneeIds().contains(keycloakId))
              .map(task -> TaskResponse.fromEntity(task, getTotalLoggedTimeById(task.getId())))
              .toList();
        } else if (role == WorkspaceRole.CLIENT) {
          Project project = findProjectByIdOrThrow(projectId);
          var clientIdOpt = clientUserRegistrationService.resolveClientId(keycloakId, tenantId);
          if (clientIdOpt.isEmpty() || !clientIdOpt.get().equals(project.getClient().getId())) {
            throw new AccessDeniedException(
                "Access Denied: You cannot view tasks for this project.");
          }
        }
      }
    }

    return tasks.stream()
        .map(task -> TaskResponse.fromEntity(task, getTotalLoggedTimeById(task.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> getTasksByAssigneeId(String assigneeId) {
    log.info("Fetching tasks for assignee: {}", assigneeId);

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()
          && roleOpt.get() == WorkspaceRole.MEMBER
          && !keycloakId.equals(assigneeId)) {
        throw new AccessDeniedException(
            "Access Denied: You cannot view task assignments of other team members.");
      }
    }

    return taskRepository.findByAssigneeId(assigneeId).stream()
        .map(task -> TaskResponse.fromEntity(task, getTotalLoggedTimeById(task.getId())))
        .toList();
  }

  @Transactional
  public TaskResponse updateTaskById(UUID id, TaskRequest request) {
    log.info("Updating task with id: {}", id);
    validateWriteAccess("update tasks");
    validateAssignees(request);
    Task task = findTaskByIdOrThrow(id);

    Project project = findProjectByIdOrThrow(request.projectId());
    task.mapFromRequestWithProject(request, project);

    Task updatedTask = taskRepository.save(task);
    return TaskResponse.fromEntity(updatedTask, getTotalLoggedTimeById(updatedTask.getId()));
  }

  @Transactional
  public TaskResponse updateTaskStatus(UUID id, TaskStatusUpdateRequest request) {
    log.info("Updating status for task with id: {}", id);
    Task task = findTaskByIdOrThrow(id);

    validateStatusUpdateAccess(task);

    task.setStatus(request.status());
    Task updated = taskRepository.save(task);
    return TaskResponse.fromEntity(updated, getTotalLoggedTimeById(updated.getId()));
  }

  @Transactional
  public void deleteTaskById(UUID id) {
    log.info("Deleting task with id: {}", id);
    validateWriteAccess("delete tasks");
    Task task = findTaskByIdOrThrow(id);
    taskRepository.delete(task);
  }

  private void validateAssignees(TaskRequest request) {
    if (request.assigneeIds() == null || request.assigneeIds().isEmpty()) {
      return;
    }
    String tenantId = TenantContextHolder.getTenantId();
    if (tenantId != null
        && userWorkspaceRepository.hasClientRoleAssignee(request.assigneeIds(), tenantId)) {
      throw new IllegalArgumentException("Clients cannot be assigned to tasks.");
    }
  }

  private void validateStatusUpdateAccess(Task task) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()) {
        WorkspaceRole role = roleOpt.get();
        if (role == WorkspaceRole.CLIENT) {
          throw new AccessDeniedException(
              "Access Denied: Clients are not allowed to update task status or move columns.");
        }
        if (role == WorkspaceRole.MEMBER && !task.getAssigneeIds().contains(keycloakId)) {
          throw new AccessDeniedException("Access Denied: You are not assigned to this task.");
        }
      }
    }
  }

  private void validateWriteAccess(String action) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()) {
        WorkspaceRole role = roleOpt.get();
        if (role == WorkspaceRole.CLIENT) {
          throw new AccessDeniedException("Access Denied: Clients cannot " + action + ".");
        }
      }
    }
  }

  private void validateUserAccessToTask(Task task) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()) {
        WorkspaceRole role = roleOpt.get();
        if (role == WorkspaceRole.MEMBER && !task.getAssigneeIds().contains(keycloakId)) {
          throw new AccessDeniedException("Access Denied: You are not assigned to this task.");
        } else if (role == WorkspaceRole.CLIENT) {
          var clientIdOpt = clientUserRegistrationService.resolveClientId(keycloakId, tenantId);
          if (clientIdOpt.isEmpty()
              || !clientIdOpt.get().equals(task.getProject().getClient().getId())) {
            throw new AccessDeniedException(
                "Access Denied: You are not authorized to access this task.");
          }
        }
      }
    }
  }

  private Task findTaskByIdOrThrow(UUID id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
  }

  private Project findProjectByIdOrThrow(UUID id) {
    return projectRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
  }

  private int getTotalLoggedTimeById(UUID id) {
    return timeEntryRepository.sumDurationMinutesByTaskId(id);
  }
}
