package dev.eyadsharkawy.agency_os_api.tenant.task.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskResponse;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskStatusUpdateRequest;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskPriority;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.TaskStatus;
import dev.eyadsharkawy.agency_os_api.tenant.task.service.TaskService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@Import(JacksonConfig.class)
public class TaskControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TaskService taskService;

  @MockitoBean(name = "workspaceSecurity")
  private WorkspaceSecurity workspaceSecurity;

  @MockitoBean private TenantSecurityFilter tenantSecurityFilter;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws Exception {
    Mockito.doAnswer(
            invocation -> {
              jakarta.servlet.ServletRequest request = invocation.getArgument(0);
              jakarta.servlet.ServletResponse response = invocation.getArgument(1);
              jakarta.servlet.FilterChain chain = invocation.getArgument(2);
              chain.doFilter(request, response);
              return null;
            })
        .when(tenantSecurityFilter)
        .doFilter(any(), any(), any());
  }

  @Test
  void testCreateTask_Success() throws Exception {
    TaskRequest request =
        new TaskRequest(
            "Task Name",
            "Desc",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.HIGH,
            TaskStatus.TODO,
            UUID.randomUUID(),
            Set.of("assigneeId"));
    TaskResponse response =
        new TaskResponse(
            UUID.randomUUID(),
            "Task Name",
            "Desc",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.HIGH,
            TaskStatus.TODO,
            UUID.randomUUID(),
            Set.of("assigneeId"),
            0,
            false,
            Instant.now(),
            Instant.now());

    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(taskService.createTask(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/tasks")
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void testGetAllTasks_Success() throws Exception {
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(taskService.getAllTasks()).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/tasks")
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testGetTaskById_Success() throws Exception {
    UUID taskId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(taskService.getTaskById(taskId))
        .thenReturn(
            new TaskResponse(
                taskId,
                "Task",
                "Desc",
                Instant.now(),
                Instant.now(),
                120,
                TaskPriority.HIGH,
                TaskStatus.TODO,
                UUID.randomUUID(),
                Set.of("user"),
                0,
                false,
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(
            get("/api/v1/tasks/{id}", taskId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testGetTasksByProjectId_Success() throws Exception {
    UUID projectId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(taskService.getTasksByProjectId(projectId)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/tasks/project/{projectId}", projectId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testGetTasksByAssigneeId_Success() throws Exception {
    String assigneeId = "user1";
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(taskService.getTasksByAssigneeId(assigneeId)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/tasks/assignee/{assigneeId}", assigneeId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testUpdateTask_Success() throws Exception {
    UUID taskId = UUID.randomUUID();
    TaskRequest request =
        new TaskRequest(
            "Updated Task",
            "Desc",
            Instant.now(),
            Instant.now(),
            120,
            TaskPriority.HIGH,
            TaskStatus.IN_PROGRESS,
            UUID.randomUUID(),
            Set.of("assigneeId"));

    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(taskService.updateTaskById(any(), any()))
        .thenReturn(
            new TaskResponse(
                taskId,
                "Updated Task",
                "Desc",
                Instant.now(),
                Instant.now(),
                120,
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                UUID.randomUUID(),
                Set.of("assigneeId"),
                0,
                false,
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(
            put("/api/v1/tasks/{id}", taskId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void testUpdateTaskStatus_Success() throws Exception {
    UUID taskId = UUID.randomUUID();
    TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.DONE);

    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(taskService.updateTaskStatus(any(), any()))
        .thenReturn(
            new TaskResponse(
                taskId,
                "Updated Task",
                "Desc",
                Instant.now(),
                Instant.now(),
                120,
                TaskPriority.HIGH,
                TaskStatus.DONE,
                UUID.randomUUID(),
                Set.of("assigneeId"),
                0,
                false,
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(
            patch("/api/v1/tasks/{id}/status", taskId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void testDeleteTask_Success() throws Exception {
    UUID taskId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);

    mockMvc
        .perform(
            delete("/api/v1/tasks/{id}", taskId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isNoContent());
  }
}
