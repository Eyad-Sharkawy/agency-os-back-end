package dev.eyadsharkawy.agency_os_api.tenant.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
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

  @InjectMocks private TaskService taskService;

  private Project project;
  private Task task;
  private UUID projectId;
  private UUID taskId;
  private Jwt jwt;

  @BeforeEach
  void setUp() {
    projectId = UUID.randomUUID();
    taskId = UUID.randomUUID();

    project = new Project();
    project.setId(projectId);
    project.setName("Project Alpha");

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
}
