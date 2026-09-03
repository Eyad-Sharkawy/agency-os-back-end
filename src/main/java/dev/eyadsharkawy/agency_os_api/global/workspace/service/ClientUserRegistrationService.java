package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.global.user.repository.AppUserRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceInvitationRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import java.util.Optional;
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
  private final WorkspaceRepository workspaceRepository;
  private final AppUserRepository userRepository;
  private final WorkspaceInvitationRepository invitationRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registerClientUser(String keycloakId, UUID clientId) {
    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Client not found with id: " + clientId));

    ClientUser clientUser =
        clientUserRepository
            .findById(keycloakId)
            .orElseGet(
                () -> {
                  ClientUser cu = new ClientUser();
                  cu.setUserId(keycloakId);
                  return cu;
                });
    clientUser.setClient(client);

    clientUserRepository.save(clientUser);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void unregisterClientUser(String keycloakId) {
    clientUserRepository.deleteById(keycloakId);
  }

  @Transactional
  public Optional<UUID> resolveClientId(String keycloakId, String tenantId) {
    var clientUserOpt = clientUserRepository.findById(keycloakId);
    if (clientUserOpt.isPresent()) {
      return Optional.of(clientUserOpt.get().getClient().getId());
    }

    if (tenantId == null
        || workspaceRepository == null
        || userRepository == null
        || invitationRepository == null) {
      return Optional.empty();
    }

    return workspaceRepository
        .findByTenantId(tenantId)
        .flatMap(
            ws ->
                userRepository
                    .findByKeycloakId(keycloakId)
                    .flatMap(
                        user ->
                            invitationRepository
                                .findByWorkspaceIdAndUsernameIgnoreCase(
                                    ws.getId(), user.getUsername())
                                .filter(
                                    inv ->
                                        inv.getRole() == WorkspaceRole.CLIENT
                                            && inv.getClientId() != null)
                                .map(
                                    inv -> {
                                      UUID clientId = inv.getClientId();
                                      clientRepository
                                          .findById(clientId)
                                          .ifPresent(
                                              client -> {
                                                ClientUser cu = new ClientUser();
                                                cu.setUserId(keycloakId);
                                                cu.setClient(client);
                                                clientUserRepository.save(cu);
                                              });
                                      return clientId;
                                    })));
  }
}
