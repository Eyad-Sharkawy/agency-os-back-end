package dev.eyadsharkawy.agency_os_api.tenant.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectRequest;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectResponse;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.ProjectStatus;
import dev.eyadsharkawy.agency_os_api.tenant.project.service.ProjectService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(ProjectController.class)
@Import(JacksonConfig.class)
public class ProjectControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ProjectService projectService;

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
  void testCreateProject_Success() throws Exception {
    ProjectRequest request =
        new ProjectRequest(
            "Project Name",
            "Project Description",
            BigDecimal.valueOf(1000),
            ProjectStatus.IN_PROGRESS,
            UUID.randomUUID(),
            BigDecimal.valueOf(100));
    ProjectResponse response =
        new ProjectResponse(
            UUID.randomUUID(),
            "Project Name",
            "Project Description",
            BigDecimal.valueOf(1000),
            ProjectStatus.IN_PROGRESS,
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            Instant.now(),
            Instant.now());

    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(projectService.createProject(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/projects")
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void testGetAllProjects_Success() throws Exception {
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(projectService.getAllProjects()).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/projects")
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testGetProjectById_Success() throws Exception {
    UUID projectId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(projectService.getProjectById(projectId))
        .thenReturn(
            new ProjectResponse(
                projectId,
                "Project",
                "Project Description",
                BigDecimal.TEN,
                ProjectStatus.IN_PROGRESS,
                UUID.randomUUID(),
                BigDecimal.TEN,
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(
            get("/api/v1/projects/{id}", projectId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testGetProjectsByClientId_Success() throws Exception {
    UUID clientId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(projectService.getProjectsByClientId(clientId)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/projects/client/{clientId}", clientId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testUpdateProject_Success() throws Exception {
    UUID projectId = UUID.randomUUID();
    ProjectRequest request =
        new ProjectRequest(
            "Updated Name",
            "Updated Description",
            BigDecimal.valueOf(2000),
            ProjectStatus.DELIVERED,
            UUID.randomUUID(),
            BigDecimal.valueOf(100));

    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(projectService.updateProjectById(any(), any()))
        .thenReturn(
            new ProjectResponse(
                projectId,
                "Updated Name",
                "Updated Description",
                BigDecimal.valueOf(2000),
                ProjectStatus.DELIVERED,
                UUID.randomUUID(),
                BigDecimal.valueOf(100),
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(
            put("/api/v1/projects/{id}", projectId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void testDeleteProject_Success() throws Exception {
    UUID projectId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);

    mockMvc
        .perform(
            delete("/api/v1/projects/{id}", projectId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isNoContent());
  }
}
