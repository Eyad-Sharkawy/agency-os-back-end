package dev.eyadsharkawy.agency_os_api.tenant.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

  @Mock private ClientRepository clientRepository;

  @Mock private ProjectRepository projectRepository;

  @InjectMocks private ClientService clientService;

  private Client client;
  private UUID clientId;

  @BeforeEach
  void setUp() {
    clientId = UUID.randomUUID();
    client = new Client();
    client.setId(clientId);
    client.setName("Globex Corp");
    client.setEmail("contact@globex.com");
    client.setStatus(ClientStatus.ACTIVE);
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
  @DisplayName("getClientById should return client response when found")
  void getClientById_Success() {
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

    ClientResponse response = clientService.getClientById(clientId);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(clientId);
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
