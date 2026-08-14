package dev.eyadsharkawy.agency_os_api.tenant.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectRequest;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectResponse;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.ProjectStatus;
import dev.eyadsharkawy.agency_os_api.tenant.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
class ProjectServiceTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ClientRepository clientRepository;
  @Mock private ClientUserRepository clientUserRepository;
  @Mock private UserWorkspaceRepository userWorkspaceRepository;

  @InjectMocks private ProjectService projectService;

  private Client client;
  private Project project;
  private UUID clientId;
  private UUID projectId;
  private Jwt jwt;

  @BeforeEach
  void setUp() {
    clientId = UUID.randomUUID();
    projectId = UUID.randomUUID();

    client = new Client();
    client.setId(clientId);
    client.setName("Globex");

    project = new Project();
    project.setId(projectId);
    project.setName("Website Redesign");
    project.setClient(client);
    project.setBudget(new BigDecimal("10000.00"));
    project.setBillingRate(new BigDecimal("150.00"));
    project.setStatus(ProjectStatus.IN_PROGRESS);

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
  @DisplayName("createProject should throw AccessDeniedException if non-OWNER assigns client")
  void createProject_NonOwner_AccessDenied() {
    mockSecurityContext(WorkspaceRole.MEMBER);
    ProjectRequest request =
        new ProjectRequest(
            "Redesign",
            "Optional Description",
            new BigDecimal("5000"),
            ProjectStatus.IN_PROGRESS,
            clientId,
            new BigDecimal("100"));

    assertThatThrownBy(() -> projectService.createProject(request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only the workspace OWNER can assign a client");
  }

  @Test
  @DisplayName("createProject should save project for OWNER")
  void createProject_Owner_Success() {
    mockSecurityContext(WorkspaceRole.OWNER);
    ProjectRequest request =
        new ProjectRequest(
            "Redesign",
            "Optional Description",
            new BigDecimal("5000"),
            ProjectStatus.IN_PROGRESS,
            clientId,
            new BigDecimal("100"));

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
    when(projectRepository.save(any(Project.class)))
        .thenAnswer(
            i -> {
              Project p = i.getArgument(0);
              p.setId(projectId);
              return p;
            });

    ProjectResponse response = projectService.createProject(request);

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Redesign");
    assertThat(response.description()).isEqualTo("Optional Description");
    assertThat(response.clientId()).isEqualTo(clientId);
  }

  @Test
  @DisplayName("getAllProjects for CLIENT role should filter by client user company")
  void getAllProjects_ClientRole_Filtered() {
    mockSecurityContext(WorkspaceRole.CLIENT);

    ClientUser clientUser = new ClientUser();
    clientUser.setUserId("kc-user-123");
    clientUser.setClient(client);

    when(clientUserRepository.findById("kc-user-123")).thenReturn(Optional.of(clientUser));
    when(projectRepository.findByClientId(clientId)).thenReturn(List.of(project));

    List<ProjectResponse> responses = projectService.getAllProjects();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(projectId);
  }

  @Test
  @DisplayName("getAllProjects for MEMBER role should filter by assigned tasks")
  void getAllProjects_MemberRole_Filtered() {
    mockSecurityContext(WorkspaceRole.MEMBER);

    when(projectRepository.findProjectsByAssigneeKeycloakId("kc-user-123"))
        .thenReturn(List.of(project));

    List<ProjectResponse> responses = projectService.getAllProjects();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(projectId);
  }

  @Test
  @DisplayName(
      "getProjectById for CLIENT role should throw AccessDeniedException if client mismatch")
  void getProjectById_ClientMismatch_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);

    Client otherClient = new Client();
    otherClient.setId(UUID.randomUUID());
    ClientUser clientUser = new ClientUser();
    clientUser.setClient(otherClient);

    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(clientUserRepository.findById("kc-user-123")).thenReturn(Optional.of(clientUser));

    assertThatThrownBy(() -> projectService.getProjectById(projectId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("You are not authorized to view this project");
  }

  @Test
  @DisplayName("getProjectById for MEMBER role should throw AccessDeniedException if not assigned")
  void getProjectById_MemberNotAssigned_AccessDenied() {
    mockSecurityContext(WorkspaceRole.MEMBER);

    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(projectRepository.isUserAssignedToProject(projectId, "kc-user-123")).thenReturn(false);

    assertThatThrownBy(() -> projectService.getProjectById(projectId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("You are not assigned to any tasks");
  }

  @Test
  @DisplayName("getProjectsByClientId should return projects for valid client")
  void getProjectsByClientId_Success() {
    when(clientRepository.existsById(clientId)).thenReturn(true);
    when(projectRepository.findByClientId(clientId)).thenReturn(List.of(project));

    List<ProjectResponse> responses = projectService.getProjectsByClientId(clientId);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(projectId);
  }

  @Test
  @DisplayName("getProjectsByClientId should throw ResourceNotFoundException when client missing")
  void getProjectsByClientId_NotFound() {
    when(clientRepository.existsById(clientId)).thenReturn(false);

    assertThatThrownBy(() -> projectService.getProjectsByClientId(clientId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("updateProjectById should update fields successfully for OWNER")
  void updateProjectById_Success() {
    mockSecurityContext(WorkspaceRole.OWNER);

    ProjectRequest request =
        new ProjectRequest(
            "New Title",
            "Updated Description",
            new BigDecimal("20000"),
            ProjectStatus.DELIVERED,
            clientId,
            new BigDecimal("200"));
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));

    ProjectResponse response = projectService.updateProjectById(projectId, request);

    assertThat(response.name()).isEqualTo("New Title");
    assertThat(response.description()).isEqualTo("Updated Description");
    assertThat(response.status()).isEqualTo(ProjectStatus.DELIVERED);
  }

  @Test
  @DisplayName("deleteProjectById should delete project when found")
  void deleteProjectById_Success() {
    when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

    projectService.deleteProjectById(projectId);

    verify(projectRepository, times(1)).delete(project);
  }
}
