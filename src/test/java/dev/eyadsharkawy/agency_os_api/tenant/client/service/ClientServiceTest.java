package dev.eyadsharkawy.agency_os_api.tenant.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.ClientUserRegistrationService;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientRequest;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientResponse;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.project.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

  @Mock private ClientRepository clientRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ClientUserRegistrationService clientUserRegistrationService;
  @Mock private UserWorkspaceRepository userWorkspaceRepository;

  @InjectMocks private ClientService clientService;

  private Client client;
  private UUID clientId;
  private Jwt jwt;

  @BeforeEach
  void setUp() {
    clientId = UUID.randomUUID();
    client = new Client();
    client.setId(clientId);
    client.setName("Globex Corp");
    client.setEmail("contact@globex.com");
    client.setStatus(ClientStatus.ACTIVE);

    jwt = mock(Jwt.class);
    lenient().when(jwt.getSubject()).thenReturn("kc-user-123");
    TenantContextHolder.setTenantId("tenant_acme");
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  private void mockSecurityContext(WorkspaceRole role) {
    org.springframework.security.core.Authentication auth =
        mock(org.springframework.security.core.Authentication.class);
    when(auth.getPrincipal()).thenReturn(jwt);

    org.springframework.security.core.context.SecurityContext securityContext =
        mock(org.springframework.security.core.context.SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(securityContext);

    when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(role));
  }

  @Test
  @DisplayName("createClient should save and return ClientResponse")
  void createClient_Success() {
    ClientRequest request =
        new ClientRequest("Globex Corp", "contact@globex.com", ClientStatus.ACTIVE);
    when(clientRepository.save(any(Client.class))).thenReturn(client);

    ClientResponse response = clientService.createClient(request);

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Globex Corp");
    assertThat(response.email()).isEqualTo("contact@globex.com");
    assertThat(response.status()).isEqualTo(ClientStatus.ACTIVE);
    verify(clientRepository, times(1)).save(any(Client.class));
  }

  @Test
  @DisplayName("getAllClients should return list of clients")
  void getAllClients_Success() {
    when(clientRepository.findAll()).thenReturn(List.of(client));

    List<ClientResponse> responses = clientService.getAllClients();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).name()).isEqualTo("Globex Corp");
  }

  @Test
  @DisplayName("getAllClients for CLIENT role should return only own client record")
  void getAllClients_ClientRole_ReturnsOwnClient() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(clientId));
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

    List<ClientResponse> responses = clientService.getAllClients();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(clientId);
    verify(clientRepository, never()).findAll();
  }

  @Test
  @DisplayName("getAllClients for CLIENT role should return empty list when no client resolved")
  void getAllClients_ClientRole_NoClient_ReturnsEmpty() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.empty());

    List<ClientResponse> responses = clientService.getAllClients();

    assertThat(responses).isEmpty();
  }

  @Test
  @DisplayName("getClientById should return client response when found")
  void getClientById_Success() {
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

    ClientResponse response = clientService.getClientById(clientId);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(clientId);
  }

  @Test
  @DisplayName("getClientById for CLIENT role should succeed when accessing own client")
  void getClientById_ClientRole_OwnClient_Success() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(clientId));
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

    ClientResponse response = clientService.getClientById(clientId);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(clientId);
  }

  @Test
  @DisplayName("getClientById for CLIENT role should throw AccessDeniedException for other client")
  void getClientById_ClientRole_OtherClient_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    UUID otherClientId = UUID.randomUUID();
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.of(otherClientId));

    assertThatThrownBy(() -> clientService.getClientById(clientId))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("You cannot view other clients");
  }

  @Test
  @DisplayName(
      "getClientById for CLIENT role should throw AccessDeniedException when client unresolved")
  void getClientById_ClientRole_NoClient_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);
    when(clientUserRegistrationService.resolveClientId("kc-user-123", "tenant_acme"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> clientService.getClientById(clientId))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
        .hasMessageContaining("You cannot view other clients");
  }

  @Test
  @DisplayName("getClientById should throw exception when client not found")
  void getClientById_NotFound() {
    when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> clientService.getClientById(clientId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Client not found");
  }

  @Test
  @DisplayName("updateClientById should update and return response when found")
  void updateClientById_Success() {
    ClientRequest request = new ClientRequest("Acme Corp", "info@acme.com", ClientStatus.INACTIVE);
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
    when(clientRepository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

    ClientResponse response = clientService.updateClientById(clientId, request);

    assertThat(response.name()).isEqualTo("Acme Corp");
    assertThat(response.email()).isEqualTo("info@acme.com");
    assertThat(response.status()).isEqualTo(ClientStatus.INACTIVE);
  }

  @Test
  @DisplayName("updateClientById should throw exception when client not found")
  void updateClientById_NotFound() {
    ClientRequest request = new ClientRequest("Acme Corp", "info@acme.com", ClientStatus.INACTIVE);
    when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> clientService.updateClientById(clientId, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("deleteClientById should delete associated projects and client")
  void deleteClientById_Success() {
    Project project = new Project();
    project.setId(UUID.randomUUID());
    project.setClient(client);

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
    when(projectRepository.findByClientId(clientId)).thenReturn(List.of(project));

    clientService.deleteClientById(clientId);

    verify(projectRepository, times(1)).delete(project);
    verify(clientRepository, times(1)).delete(client);
  }

  @Test
  @DisplayName("deleteClientById should throw exception when client not found")
  void deleteClientById_NotFound() {
    when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> clientService.deleteClientById(clientId))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
