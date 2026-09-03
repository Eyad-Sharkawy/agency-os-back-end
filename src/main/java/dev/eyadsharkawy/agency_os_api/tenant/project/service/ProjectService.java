package dev.eyadsharkawy.agency_os_api.tenant.project.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.ClientUserRegistrationService;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectRequest;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectResponse;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
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
public class ProjectService {

  private static final String PROJECT_NOT_FOUND_PREFIX = "Project not found with id: ";

  private final ProjectRepository projectRepository;
  private final ClientRepository clientRepository;
  private final ClientUserRegistrationService clientUserRegistrationService;
  private final UserWorkspaceRepository userWorkspaceRepository;

  @Transactional
  public ProjectResponse createProject(ProjectRequest request) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();
      WorkspaceRole role =
          userWorkspaceRepository
              .findRoleByKeycloakIdAndTenantId(keycloakId, tenantId)
              .orElseThrow(
                  () -> new AccessDeniedException("Access Denied: Requester is not a member."));

      if (role == WorkspaceRole.CLIENT) {
        throw new AccessDeniedException(
            "Access Denied: Clients are not allowed to create projects.");
      }

      if (role != WorkspaceRole.OWNER && request.clientId() != null) {
        throw new AccessDeniedException(
            "Only the workspace OWNER can assign a client to a project.");
      }
    }

    Client client =
        clientRepository
            .findById(request.clientId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Client not found with id: " + request.clientId()));

    Project project = new Project();
    project.setName(request.name());
    project.setDescription(request.description());
    project.setBudget(request.budget());
    project.setStatus(request.status());
    project.setClient(client);
    project.setBillingRate(request.billingRate());

    Project savedProject = projectRepository.save(project);
    return ProjectResponse.fromEntity(savedProject);
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> getAllProjects() {
    log.info("Fetching projects");

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()) {
        WorkspaceRole role = roleOpt.get();
        if (role == WorkspaceRole.CLIENT) {
          var clientIdOpt = clientUserRegistrationService.resolveClientId(keycloakId, tenantId);
          if (clientIdOpt.isPresent()) {
            UUID clientId = clientIdOpt.get();
            log.info(
                "Client portal user [{}] queried projects. Filtering for client [{}]",
                keycloakId,
                clientId);
            return projectRepository.findByClientId(clientId).stream()
                .map(ProjectResponse::fromEntity)
                .toList();
          }
          return List.of(); // If the client user has no linked company, return empty
        } else if (role == WorkspaceRole.MEMBER) {
          log.info("Member user [{}] queried projects. Filtering by task assignments.", keycloakId);
          return projectRepository.findProjectsByAssigneeKeycloakId(keycloakId).stream()
              .map(ProjectResponse::fromEntity)
              .toList();
        }
      }
    }

    return projectRepository.findAll().stream().map(ProjectResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public ProjectResponse getProjectById(UUID id) {
    log.info("Fetching project with id: {}", id);

    Project project =
        projectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND_PREFIX + id));

    validateProjectAccess(project, id);

    return ProjectResponse.fromEntity(project);
  }

  private void validateProjectAccess(Project project, UUID id) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();

      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent()) {
        WorkspaceRole role = roleOpt.get();
        if (role == WorkspaceRole.CLIENT) {
          validateClientProjectAccess(project, keycloakId, tenantId);
        } else if (role == WorkspaceRole.MEMBER
            && !projectRepository.isUserAssignedToProject(id, keycloakId)) {
          throw new AccessDeniedException(
              "Access Denied: You are not assigned to any tasks in this project.");
        }
      }
    }
  }

  private void validateClientProjectAccess(Project project, String keycloakId, String tenantId) {
    var clientIdOpt = clientUserRegistrationService.resolveClientId(keycloakId, tenantId);
    if (clientIdOpt.isEmpty() || !clientIdOpt.get().equals(project.getClient().getId())) {
      throw new AccessDeniedException(
          "Access Denied: You are not authorized to view this project.");
    }
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> getProjectsByClientId(UUID clientId) {
    log.info("Fetching projects for client: {}", clientId);
    if (!clientRepository.existsById(clientId)) {
      throw new ResourceNotFoundException("Client not found with id: " + clientId);
    }

    return projectRepository.findByClientId(clientId).stream()
        .map(ProjectResponse::fromEntity)
        .toList();
  }

  @Transactional
  public ProjectResponse updateProjectById(UUID id, ProjectRequest request) {
    log.info("Updating project with id: {}", id);

    Project project =
        projectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND_PREFIX + id));

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();
      WorkspaceRole role =
          userWorkspaceRepository
              .findRoleByKeycloakIdAndTenantId(keycloakId, tenantId)
              .orElseThrow(
                  () -> new AccessDeniedException("Access Denied: Requester is not a member."));

      if (role == WorkspaceRole.CLIENT) {
        throw new AccessDeniedException(
            "Access Denied: Clients are not allowed to update projects.");
      }

      if (role != WorkspaceRole.OWNER
          && request.clientId() != null
          && !request.clientId().equals(project.getClient().getId())) {
        throw new AccessDeniedException("Only the workspace OWNER can change a project's client.");
      }
    }

    if (!project.getClient().getId().equals(request.clientId())) {
      Client client =
          clientRepository
              .findById(request.clientId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Client not found with id: " + request.clientId()));

      project.setClient(client);
    }

    project.setName(request.name());
    project.setDescription(request.description());
    project.setBudget(request.budget());
    project.setStatus(request.status());
    project.setBillingRate(request.billingRate());

    Project savedProject = projectRepository.save(project);
    return ProjectResponse.fromEntity(savedProject);
  }

  @Transactional
  public void deleteProjectById(UUID id) {
    log.info("Deleting project with id: {}", id);
    Project project =
        projectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND_PREFIX + id));

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      String keycloakId = jwt.getSubject();
      String tenantId = TenantContextHolder.getTenantId();
      var roleOpt = userWorkspaceRepository.findRoleByKeycloakIdAndTenantId(keycloakId, tenantId);
      if (roleOpt.isPresent() && roleOpt.get() == WorkspaceRole.CLIENT) {
        throw new AccessDeniedException(
            "Access Denied: Clients are not allowed to delete projects.");
      }
    }

    projectRepository.delete(project);
  }
}
