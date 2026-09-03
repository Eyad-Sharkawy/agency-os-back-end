package dev.eyadsharkawy.agency_os_api.tenant.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskPriority;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;
import dev.eyadsharkawy.agency_os_api.tenant.task.repository.TaskRepository;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository.TimeEntryRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private UserWorkspaceRepository userWorkspaceRepository;
  @Mock private ClientUserRegistrationService clientUserRegistrationService;

  @InjectMocks private TaskService taskService;

  private Project project;
  private Task task;
  private UUID projectId;
  private UUID taskId;
  private UUID clientId;
  private Jwt jwt;

  @BeforeEach
  void setUp() {
    projectId = UUID.randomUUID();
    taskId = UUID.randomUUID();
    clientId = UUID.randomUUID();

    dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client client =
        new dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client();
    client.setId(clientId);

    project = new Project();
    project.setId(projectId);
    project.setName("Project Alpha");
    project.setClient(client);

    task = new Task();
    task.setId(taskId);
    task.setTitle("Design Mockups");
    task.setDescription("Create UI components");
    task.setStatus(TaskStatus.IN_PROGRESS);
    task.setPriority(TaskPriority.HIGH);
    task.setProject(project);
    task.setAssigneeIds(new HashSet<>(Set.of("kc-user-123")));

    jwt = mock(Jwt.class);
    lenient().when(jwt.getSubject()).thenReturn("kc-user-123");

    TenantContextHolder.setTenantId("tenant_acme");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  private void mockSecurityContext(WorkspaceRole role) {
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(jwt);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(securityContext);

    if (role != null) {
      when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
          .thenReturn(Optional.of(role));
    }
  }

  @Test
  @DisplayName("createTask should throw ResourceNotFoundException when project not found")
  void createTask_ProjectNotFound() {
    TaskRequest request =
        new TaskRequest(
            "Title",
            "Desc",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.HIGH,
            TaskStatus.TODO,
            projectId,
            Set.of("kc-user-123"));
    when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.createTask(request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("createTask should save task and return TaskResponse")
  void createTask_Success() {
    TaskRequest request =
        new TaskRequest(
            "Design Mockups",
            "Create UI components",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.HIGH,
            TaskStatus.IN_PROGRESS,
            projectId,
            Set.of("kc-user-123"));
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(taskRepository.save(any(Task.class)))
        .thenAnswer(
            i -> {
              Task t = i.getArgument(0);
              t.setId(taskId);
              return t;
            });
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(60);

    TaskResponse response = taskService.createTask(request);

    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo("Design Mockups");
    assertThat(response.totalLoggedMinutes()).isEqualTo(60);
  }

  @Test
  @DisplayName("getAllTasks for MEMBER role should filter by assignee")
  void getAllTasks_MemberRole_Filtered() {
    mockSecurityContext(WorkspaceRole.MEMBER);
    when(taskRepository.findByAssigneeId("kc-user-123")).thenReturn(List.of(task));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(30);

    List<TaskResponse> responses = taskService.getAllTasks();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(taskId);
  }

  @Test
  @DisplayName(
      "getTaskById for MEMBER role should throw AccessDeniedException if user not assigned")
  void getTaskById_MemberNotAssigned_AccessDenied() {
    mockSecurityContext(WorkspaceRole.MEMBER);
    task.setAssigneeIds(Set.of("other-user"));

    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> taskService.getTaskById(taskId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("You are not assigned to this task");
  }

  @Test
  @DisplayName(
      "getTasksByAssigneeId for MEMBER querying another assignee should throw AccessDeniedException")
  void getTasksByAssigneeId_MemberQueryingOther_AccessDenied() {
    mockSecurityContext(WorkspaceRole.MEMBER);

    assertThatThrownBy(() -> taskService.getTasksByAssigneeId("other-user-999"))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("cannot view task assignments of other team members");
  }

  @Test
  @DisplayName("updateTaskStatus should update status successfully")
  void updateTaskStatus_Success() {
    mockSecurityContext(WorkspaceRole.MEMBER);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(90);

    TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);
    TaskResponse response = taskService.updateTaskStatus(taskId, request);

    assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    verify(taskRepository, times(1)).save(task);
  }

  @Test
  @DisplayName("deleteTaskById should delete task when found")
  void deleteTaskById_Success() {
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

    taskService.deleteTaskById(taskId);

    verify(taskRepository, times(1)).delete(task);
  }

  @Test
  @DisplayName("getTasksByAssigneeId for MEMBER querying self should succeed")
  void getTasksByAssigneeId_MemberQueryingSelf_Success() {
    mockSecurityContext(WorkspaceRole.MEMBER);
    when(taskRepository.findByAssigneeId("kc-user-123")).thenReturn(List.of(task));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(60);

    List<TaskResponse> responses = taskService.getTasksByAssigneeId("kc-user-123");

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(taskId);
  }

  @Test
  @DisplayName("getTasksByAssigneeId for ADMIN querying other user should succeed")
  void getTasksByAssigneeId_AdminQueryingOther_Success() {
    mockSecurityContext(WorkspaceRole.ADMIN);
    when(taskRepository.findByAssigneeId("other-user-999")).thenReturn(List.of(task));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(60);

    List<TaskResponse> responses = taskService.getTasksByAssigneeId("other-user-999");

    assertThat(responses).hasSize(1);
  }

  @Test
  @DisplayName("updateTaskStatus should throw AccessDeniedException when member not assigned")
  void updateTaskStatus_MemberNotAssigned_AccessDenied() {
    mockSecurityContext(WorkspaceRole.MEMBER);
    task.setAssigneeIds(Set.of("someone-else"));
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

    TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);

    assertThatThrownBy(() -> taskService.updateTaskStatus(taskId, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("You are not assigned to this task");
  }

  @Test
  @DisplayName("updateTaskById should update and return response")
  void updateTaskById_Success() {
    TaskRequest request =
        new TaskRequest(
            "New Task Title",
            "New Desc",
            Instant.now(),
            Instant.now(),
            180,
            TaskPriority.URGENT,
            TaskStatus.IN_PROGRESS,
            projectId,
            Set.of("kc-user-123"));

    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(120);

    TaskResponse response = taskService.updateTaskById(taskId, request);

    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo("New Task Title");
  }

  @Test
  @DisplayName("updateTaskStatus should throw AccessDeniedException when client attempts update")
  void updateTaskStatus_ClientRole_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

    TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);

    assertThatThrownBy(() -> taskService.updateTaskStatus(taskId, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Clients are not allowed to update task status or move columns");
  }

  @Test
  @DisplayName("createTask should throw AccessDeniedException when client attempts to create task")
  void createTask_ClientRole_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    TaskRequest request =
        new TaskRequest(
            "Client Task",
            "Desc",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.MEDIUM,
            TaskStatus.TODO,
            projectId,
            Set.of());

    assertThatThrownBy(() -> taskService.createTask(request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Clients cannot create tasks");
  }

  @Test
  @DisplayName("createTask should throw IllegalArgumentException when client assigned")
  void createTask_ClientAssignee_ThrowsException() {
    when(userWorkspaceRepository.hasClientRoleAssignee(any(), eq("tenant_acme"))).thenReturn(true);
    TaskRequest request =
        new TaskRequest(
            "Task",
            "Desc",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.MEDIUM,
            TaskStatus.TODO,
            projectId,
            Set.of("kc-client-123"));

    assertThatThrownBy(() -> taskService.createTask(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Clients cannot be assigned to tasks");
  }

  @Test
  @DisplayName("getAllTasks for CLIENT role should return tasks for client projects")
  void getAllTasks_ClientRole_WithClient_Success() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(clientId));
    when(taskRepository.findByProjectClientId(clientId)).thenReturn(List.of(task));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(60);

    List<TaskResponse> responses = taskService.getAllTasks();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(taskId);
  }

  @Test
  @DisplayName("getAllTasks for CLIENT role should return empty list when no client resolved")
  void getAllTasks_ClientRole_NoClient_ReturnsEmpty() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.empty());

    List<TaskResponse> responses = taskService.getAllTasks();

    assertThat(responses).isEmpty();
  }

  @Test
  @DisplayName("getTaskById for CLIENT role should succeed when task belongs to client project")
  void getTaskById_ClientRole_OwnClient_Success() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(clientId));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(60);

    TaskResponse response = taskService.getTaskById(taskId);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(taskId);
  }

  @Test
  @DisplayName("getTaskById for CLIENT role should throw AccessDeniedException when other client")
  void getTaskById_ClientRole_OtherClient_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(UUID.randomUUID()));

    assertThatThrownBy(() -> taskService.getTaskById(taskId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("not authorized to access this task");
  }

  @Test
  @DisplayName(
      "getTaskById for CLIENT role should throw AccessDeniedException when client unresolved")
  void getTaskById_ClientRole_NoClient_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.getTaskById(taskId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("not authorized to access this task");
  }

  @Test
  @DisplayName("getTasksByProjectId should throw ResourceNotFoundException when project not found")
  void getTasksByProjectId_ProjectNotFound_ThrowsException() {
    when(projectRepository.existsById(projectId)).thenReturn(false);

    assertThatThrownBy(() -> taskService.getTasksByProjectId(projectId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Project not found with id: " + projectId);
  }

  @Test
  @DisplayName("getTasksByProjectId for MEMBER role should filter by assigned tasks")
  void getTasksByProjectId_MemberRole_FiltersByAssigned() {
    mockSecurityContext(WorkspaceRole.MEMBER);
    when(projectRepository.existsById(projectId)).thenReturn(true);

    Task unassignedTask = new Task();
    unassignedTask.setId(UUID.randomUUID());
    unassignedTask.setAssigneeIds(Set.of("other-user-999"));

    when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(task, unassignedTask));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(30);

    List<TaskResponse> responses = taskService.getTasksByProjectId(projectId);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(taskId);
  }

  @Test
  @DisplayName("getTasksByProjectId for CLIENT role should succeed when project belongs to client")
  void getTasksByProjectId_ClientRole_OwnProject_Success() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(projectRepository.existsById(projectId)).thenReturn(true);
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(clientId));
    when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(task));
    when(timeEntryRepository.sumDurationMinutesByTaskId(taskId)).thenReturn(30);

    List<TaskResponse> responses = taskService.getTasksByProjectId(projectId);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(taskId);
  }

  @Test
  @DisplayName(
      "getTasksByProjectId for CLIENT role should throw AccessDeniedException for other project")
  void getTasksByProjectId_ClientRole_OtherProject_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(projectRepository.existsById(projectId)).thenReturn(true);
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(UUID.randomUUID()));

    assertThatThrownBy(() -> taskService.getTasksByProjectId(projectId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("You cannot view tasks for this project");
  }

  @Test
  @DisplayName("updateTaskById for CLIENT role should throw AccessDeniedException")
  void updateTaskById_ClientRole_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    TaskRequest request =
        new TaskRequest(
            "Update",
            "Desc",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.MEDIUM,
            TaskStatus.IN_PROGRESS,
            projectId,
            Set.of());

    assertThatThrownBy(() -> taskService.updateTaskById(taskId, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Clients cannot update tasks");
  }

  @Test
  @DisplayName("deleteTaskById for CLIENT role should throw AccessDeniedException")
  void deleteTaskById_ClientRole_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);

    assertThatThrownBy(() -> taskService.deleteTaskById(taskId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Clients cannot delete tasks");
  }
}
