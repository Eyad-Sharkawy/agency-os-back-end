package dev.eyadsharkawy.agency_os_api.global.workspace.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.WorkspaceInvitationService;
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

@WebMvcTest(WorkspaceInvitationController.class)
@Import(JacksonConfig.class)
class WorkspaceInvitationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WorkspaceInvitationService invitationService;

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
  void testInviteUser_Success() throws Exception {
    String tenantId = "tenant1";
    WorkspaceInvitationRequest request =
        new WorkspaceInvitationRequest("user@test.com", null, WorkspaceRole.MEMBER, null);
    WorkspaceInvitationResponse response =
        new WorkspaceInvitationResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test Workspace",
            "user_test",
            "admin_user",
            "MEMBER",
            null,
            "PENDING",
            Instant.now());

    when(workspaceSecurity.hasRoleInTenant(anyString(), any(String[].class))).thenReturn(true);
    when(invitationService.inviteUser(any(), eq(tenantId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/workspaces/{tenantId}/invitations", tenantId)
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void testGetPendingInvitations_Success() throws Exception {
    when(invitationService.getPendingInvitations(any())).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/workspaces/invitations").with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testAcceptInvitation_Success() throws Exception {
    UUID invitationId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/workspaces/invitations/{invitationId}/accept", invitationId)
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testDeclineInvitation_Success() throws Exception {
    UUID invitationId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/workspaces/invitations/{invitationId}/decline", invitationId)
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }
}
