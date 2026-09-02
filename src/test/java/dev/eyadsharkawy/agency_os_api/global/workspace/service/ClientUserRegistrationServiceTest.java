package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
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

  @InjectMocks private ClientUserRegistrationService registrationService;

  @Test
  @DisplayName("registerClientUser should save ClientUser when client exists")
  void registerClientUser_Success() {
    UUID clientId = UUID.randomUUID();
    String keycloakId = "kc-user-123";
    Client client = new Client();
    client.setId(clientId);

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

    registrationService.registerClientUser(keycloakId, clientId);

    ArgumentCaptor<ClientUser> captor = ArgumentCaptor.forClass(ClientUser.class);
    verify(clientUserRepository, times(1)).save(captor.capture());

    ClientUser saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(keycloakId);
    assertThat(saved.getClient()).isEqualTo(client);
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
}
