package dev.eyadsharkawy.agency_os_api.tenant.client.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientRequest;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientResponse;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.project.repository.ProjectRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {
  private final ClientRepository clientRepository;
  private final ProjectRepository projectRepository;

  @Transactional
  public ClientResponse createClient(ClientRequest request) {
    log.info("Creating client with name: {}", request.name());

    Client client = new Client();
    client.setName(request.name());
    client.setEmail(request.email());
    client.setStatus(request.status());

    Client savedClient = clientRepository.save(client);

    return ClientResponse.fromEntity(savedClient);
  }

  @Transactional(readOnly = true)
  public List<ClientResponse> getAllClients() {
    log.info("Fetching all clients for the current tenant");

    return clientRepository.findAll().stream().map(ClientResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public ClientResponse getClientById(UUID id) {
    log.info("Fetching client with id: {}", id);
    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

    return ClientResponse.fromEntity(client);
  }

  @Transactional
  public ClientResponse updateClientById(UUID id, ClientRequest request) {
    log.info("Updating client with id: {}", id);

    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

    client.setName(request.name());
    client.setEmail(request.email());
    client.setStatus(request.status());

    Client savedClient = clientRepository.save(client);
    return ClientResponse.fromEntity(savedClient);
  }

  @Transactional
  public void deleteClientById(UUID id) {
    log.info("Deleting client with id: {}", id);

    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

    projectRepository.findByClientId(client.getId()).forEach(projectRepository::delete);

    clientRepository.delete(client);
  }
}
