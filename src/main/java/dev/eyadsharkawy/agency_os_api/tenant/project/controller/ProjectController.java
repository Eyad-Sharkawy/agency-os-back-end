package dev.eyadsharkawy.agency_os_api.tenant.project.controller;

import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectRequest;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectResponse;
import dev.eyadsharkawy.agency_os_api.tenant.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> responses = projectService.getAllProjects();
        return ResponseEntity.ok(responses);
    }

    // Owners, Admins, Members, and Clients can read specific projects (with Client ID checks inside the service layer)
    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    // Owners, Admins, Members can query projects by client ID
    @GetMapping("/client/{clientId}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER')")
    public ResponseEntity<List<ProjectResponse>> getProjectsByClientId(@PathVariable UUID clientId) {
        List<ProjectResponse> responses = projectService.getProjectsByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    // Owners and Admins can update projects
    @PutMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.updateProjectById(id, request);
        return ResponseEntity.ok(response);
    }

    // Only OWNER can delete projects
    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProjectById(id);
        return ResponseEntity.noContent().build();
    }
}
