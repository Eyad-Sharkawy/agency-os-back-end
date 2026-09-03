package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceInvitation;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceInvitationRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientUserRegistrationServiceTest {

  @Mock private ClientRepository clientRepository;
  @Mock private ClientUserRepository clientUserRepository;
  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private AppUserRepository userRepository;
  @Mock private WorkspaceInvitationRepository invitationRepository;

  @InjectMocks private ClientUserRegistrationService registrationService;

  @Test
  @DisplayName("registerClientUser should save ClientUser when client exists and no existing user")
  void registerClientUser_Success() {
    UUID clientId = UUID.randomUUID();
    String keycloakId = "kc-user-123";
    Client client = new Client();
    client.setId(clientId);

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());

    registrationService.registerClientUser(keycloakId, clientId);

    ArgumentCaptor<ClientUser> captor = ArgumentCaptor.forClass(ClientUser.class);
    verify(clientUserRepository, times(1)).save(captor.capture());

    ClientUser saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(keycloakId);
    assertThat(saved.getClient()).isEqualTo(client);
  }

  @Test
  @DisplayName("registerClientUser should update existing ClientUser when client exists")
  void registerClientUser_ExistingUser_Success() {
    UUID clientId = UUID.randomUUID();
    String keycloakId = "kc-user-123";
    Client client = new Client();
    client.setId(clientId);

    ClientUser existing = new ClientUser();
    existing.setUserId(keycloakId);

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.of(existing));

    registrationService.registerClientUser(keycloakId, clientId);

    verify(clientUserRepository, times(1)).save(existing);
    assertThat(existing.getClient()).isEqualTo(client);
  }

  @Test
  @DisplayName(
      "registerClientUser should throw ResourceNotFoundException when client does not exist")
  void registerClientUser_ClientNotFound_ThrowsException() {
    UUID clientId = UUID.randomUUID();
    String keycloakId = "kc-user-123";

    when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> registrationService.registerClientUser(keycloakId, clientId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Client not found with id: " + clientId);

    verify(clientUserRepository, never()).save(any(ClientUser.class));
  }

  @Test
  @DisplayName("unregisterClientUser should delete by keycloakId")
  void unregisterClientUser_Success() {
    String keycloakId = "kc-user-123";

    registrationService.unregisterClientUser(keycloakId);

    verify(clientUserRepository, times(1)).deleteById(keycloakId);
  }

  @Test
  @DisplayName("resolveClientId should return clientId directly when ClientUser exists in tenant")
  void resolveClientId_ClientUserExists_ReturnsClientId() {
    String keycloakId = "kc-user-123";
    String tenantId = "tenant_acme";
    UUID clientId = UUID.randomUUID();

    Client client = new Client();
    client.setId(clientId);

    ClientUser clientUser = new ClientUser();
    clientUser.setUserId(keycloakId);
    clientUser.setClient(client);

    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.of(clientUser));

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, tenantId);

    assertThat(result).contains(clientId);
    verifyNoInteractions(workspaceRepository);
  }

  @Test
  @DisplayName("resolveClientId should return empty when tenantId is null")
  void resolveClientId_TenantIdNull_ReturnsEmpty() {
    String keycloakId = "kc-user-123";
    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, null);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveClientId should return empty when workspace not found")
  void resolveClientId_WorkspaceNotFound_ReturnsEmpty() {
    String keycloakId = "kc-user-123";
    String tenantId = "tenant_acme";

    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());
    when(workspaceRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, tenantId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveClientId should return empty when user not found")
  void resolveClientId_UserNotFound_ReturnsEmpty() {
    String keycloakId = "kc-user-123";
    String tenantId = "tenant_acme";

    Workspace ws = new Workspace();
    ws.setId(UUID.randomUUID());

    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());
    when(workspaceRepository.findByTenantId(tenantId)).thenReturn(Optional.of(ws));
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, tenantId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveClientId should return empty when invitation is not found")
  void resolveClientId_InvitationNotFound_ReturnsEmpty() {
    String keycloakId = "kc-user-123";
    String tenantId = "tenant_acme";

    Workspace ws = new Workspace();
    ws.setId(UUID.randomUUID());

    AppUser user = new AppUser();
    user.setUsername("client_john");

    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());
    when(workspaceRepository.findByTenantId(tenantId)).thenReturn(Optional.of(ws));
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
    when(invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(ws.getId(), "client_john"))
        .thenReturn(Optional.empty());

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, tenantId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("resolveClientId should return empty when invitation is not CLIENT role")
  void resolveClientId_InvitationNotClientRole_ReturnsEmpty() {
    String keycloakId = "kc-user-123";
    String tenantId = "tenant_acme";

    Workspace ws = new Workspace();
    ws.setId(UUID.randomUUID());

    AppUser user = new AppUser();
    user.setUsername("client_john");

    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setRole(WorkspaceRole.MEMBER);
    inv.setClientId(null);

    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());
    when(workspaceRepository.findByTenantId(tenantId)).thenReturn(Optional.of(ws));
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
    when(invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(ws.getId(), "client_john"))
        .thenReturn(Optional.of(inv));

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, tenantId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName(
      "resolveClientId should auto-backfill ClientUser and return clientId from invitation")
  void resolveClientId_InvitationFound_BackfillsClientUserAndReturnsClientId() {
    String keycloakId = "kc-user-123";
    String tenantId = "tenant_acme";
    UUID clientId = UUID.randomUUID();

    Workspace ws = new Workspace();
    ws.setId(UUID.randomUUID());

    AppUser user = new AppUser();
    user.setUsername("client_john");

    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setRole(WorkspaceRole.CLIENT);
    inv.setClientId(clientId);

    Client client = new Client();
    client.setId(clientId);

    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());
    when(workspaceRepository.findByTenantId(tenantId)).thenReturn(Optional.of(ws));
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
    when(invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(ws.getId(), "client_john"))
        .thenReturn(Optional.of(inv));
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, tenantId);

    assertThat(result).contains(clientId);
    ArgumentCaptor<ClientUser> captor = ArgumentCaptor.forClass(ClientUser.class);
    verify(clientUserRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(keycloakId);
    assertThat(captor.getValue().getClient()).isEqualTo(client);
  }

  @Test
  @DisplayName("resolveClientId should return clientId even if client entity missing in tenant")
  void resolveClientId_InvitationFound_ClientEntityMissing_ReturnsClientId() {
    String keycloakId = "kc-user-123";
    String tenantId = "tenant_acme";
    UUID clientId = UUID.randomUUID();

    Workspace ws = new Workspace();
    ws.setId(UUID.randomUUID());

    AppUser user = new AppUser();
    user.setUsername("client_john");

    WorkspaceInvitation inv = new WorkspaceInvitation();
    inv.setRole(WorkspaceRole.CLIENT);
    inv.setClientId(clientId);

    when(clientUserRepository.findById(keycloakId)).thenReturn(Optional.empty());
    when(workspaceRepository.findByTenantId(tenantId)).thenReturn(Optional.of(ws));
    when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
    when(invitationRepository.findByWorkspaceIdAndUsernameIgnoreCase(ws.getId(), "client_john"))
        .thenReturn(Optional.of(inv));
    when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

    Optional<UUID> result = registrationService.resolveClientId(keycloakId, tenantId);

    assertThat(result).contains(clientId);
    verify(clientUserRepository, never()).save(any());
  }
}
