package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientUserRegistrationService {

  private final ClientRepository clientRepository;
  private final ClientUserRepository clientUserRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registerClientUser(String keycloakId, UUID clientId) {
    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Client not found with id: " + clientId));

    ClientUser clientUser = new ClientUser();
    clientUser.setUserId(keycloakId);
    clientUser.setClient(client);

    clientUserRepository.save(clientUser);
  }
}
