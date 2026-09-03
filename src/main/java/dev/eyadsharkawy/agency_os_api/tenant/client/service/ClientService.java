package dev.eyadsharkawy.agency_os_api.tenant.client.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.ClientUserRegistrationService;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientRequest;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientResponse;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.project.repository.ProjectRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {
  private static final String CLIENT_NOT_FOUND_PREFIX = "Client not found with id: ";

  private final ClientRepository clientRepository;
  private final ProjectRepository projectRepository;
  private final ClientUserRegistrationService clientUserRegistrationService;
  private final UserWorkspaceRepository userWorkspaceRepository;

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

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent() && roleOpt.get() == WorkspaceRole.CLIENT) {
        log.info("Client user [{}] queried clients. Restricting to own client record.", keycloakId);
        var clientIdOpt = clientUserRegistrationService.resolveClientId(keycloakId, tenantId);
        if (clientIdOpt.isPresent()) {
          return clientRepository.findById(clientIdOpt.get()).stream()
              .map(ClientResponse::fromEntity)
              .toList();
        }
        return List.of();
      }
    }

    return clientRepository.findAll().stream().map(ClientResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public ClientResponse getClientById(UUID id) {
    log.info("Fetching client with id: {}", id);

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent() && roleOpt.get() == WorkspaceRole.CLIENT) {
        var clientIdOpt = clientUserRegistrationService.resolveClientId(keycloakId, tenantId);
        if (clientIdOpt.isEmpty() || !clientIdOpt.get().equals(id)) {
          throw new AccessDeniedException("Access Denied: You cannot view other clients.");
        }
      }
    }

    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CLIENT_NOT_FOUND_PREFIX + id));

    return ClientResponse.fromEntity(client);
  }

  @Transactional
  public ClientResponse updateClientById(UUID id, ClientRequest request) {
    log.info("Updating client with id: {}", id);

    Client client =
        clientRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CLIENT_NOT_FOUND_PREFIX + id));

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
            .orElseThrow(() -> new ResourceNotFoundException(CLIENT_NOT_FOUND_PREFIX + id));

    projectRepository.findByClientId(client.getId()).forEach(projectRepository::delete);

    clientRepository.delete(client);
  }
}
