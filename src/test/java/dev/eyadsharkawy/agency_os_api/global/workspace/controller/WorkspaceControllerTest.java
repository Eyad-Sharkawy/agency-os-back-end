package dev.eyadsharkawy.agency_os_api.global.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.WorkspaceService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceController.class)
@Import(JacksonConfig.class)
public class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceService workspaceService;

    @MockitoBean(name = "workspaceSecurity")
    private WorkspaceSecurity workspaceSecurity;

    @MockitoBean
    private TenantSecurityFilter tenantSecurityFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(tenantSecurityFilter).doFilter(any(), any(), any());
    }

    @Test
    void testCreateWorkspace_Success() throws Exception {
        WorkspaceRequest request = new WorkspaceRequest("Test Workspace");
        WorkspaceResponse response = new WorkspaceResponse(UUID.randomUUID(), "Test Workspace", "tenant1", "test@test.com", "OWNER", true, Instant.now(), Instant.now());

        when(workspaceService.createWorkspace(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetUserWorkspaces_Success() throws Exception {
        when(workspaceService.getUserWorkspaces(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/workspaces")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateWorkspace_Success() throws Exception {
        String tenantId = "tenant1";
        WorkspaceRequest request = new WorkspaceRequest("Updated Workspace");

        when(workspaceSecurity.hasRole(anyString(), any(String[].class))).thenReturn(true);
        when(workspaceService.updateUserWorkspaceByTenantId(eq(tenantId), any())).thenReturn(new WorkspaceResponse(UUID.randomUUID(), "Updated", tenantId, "updated@test.com", "OWNER", true, Instant.now(), Instant.now()));

        mockMvc.perform(put("/api/v1/workspaces/{tenantId}", tenantId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteWorkspace_Success() throws Exception {
        String tenantId = "tenant1";

        when(workspaceSecurity.hasRole(anyString(), any(String[].class))).thenReturn(true);

        mockMvc.perform(delete("/api/v1/workspaces/{tenantId}", tenantId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNoContent());
    }
}

