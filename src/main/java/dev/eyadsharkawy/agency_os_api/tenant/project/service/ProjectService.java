package dev.eyadsharkawy.agency_os_api.tenant.project.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectRequest;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectResponse;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {
    final private ProjectRepository projectRepository;
    final private ClientRepository clientRepository;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));

        Project project = new Project();

        project.setName(request.name());
        project.setBudget(request.budget());
        project.setStatus(request.status());
        project.setClient(client);

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all projects");
        return projectRepository.findAll().stream()
                .map(ProjectResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        log.info("Fetching project with id: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        return ProjectResponse.fromEntity(project);
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

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        if (!project.getClient().getId().equals(request.clientId())) {
            Client client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));

            project.setClient(client);
        }

        project.setName(request.name());
        project.setBudget(request.budget());
        project.setStatus(request.status());

        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public void deleteProjectById(UUID id) {
        log.info("Deleting project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        projectRepository.delete(project);
    }
}
