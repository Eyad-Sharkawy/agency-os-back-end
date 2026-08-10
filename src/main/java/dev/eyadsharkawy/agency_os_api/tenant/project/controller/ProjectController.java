package dev.eyadsharkawy.agency_os_api.tenant.project.controller;

import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectRequest;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectResponse;
import dev.eyadsharkawy.agency_os_api.tenant.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "04. Projects", description = "Endpoints for managing projects, budgets, billable rates, and task-based teammate security mappings")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
    @Operation(summary = "Create project", description = "Initializes a new project under the current tenant. Only the OWNER can assign a client company during creation; Admins cannot.")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')")
    @Operation(summary = "List all projects", description = "Retrieves all projects. CLIENT users only see projects belonging to their company; MEMBER users only see projects where they hold at least one assigned task.")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> responses = projectService.getAllProjects();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER', 'CLIENT')")
    @Operation(summary = "Get project by ID", description = "Retrieves detailed information of a project. Verifies that CLIENT users belong to the linked company, and MEMBER users hold assigned tasks.")
    public ResponseEntity<ProjectResponse> getProjectById(
            @Parameter(description = "The project unique ID") @PathVariable UUID id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER')")
    @Operation(summary = "Get projects by Client ID", description = "Lists all projects associated with a specific client company. Restricted to OWNER, ADMIN, or MEMBER.")
    public ResponseEntity<List<ProjectResponse>> getProjectsByClientId(
            @Parameter(description = "The client company unique ID") @PathVariable UUID clientId) {
        List<ProjectResponse> responses = projectService.getProjectsByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update project details", description = "Modifies project parameters. Only the OWNER is allowed to change or assign client mappings; Admins are blocked. Restricted to OWNER or ADMIN.")
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "The project unique ID") @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.updateProjectById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    @Operation(summary = "Delete project", description = "Permanently deletes a project from the system. Restricted strictly to the OWNER.")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "The project unique ID") @PathVariable UUID id) {
        projectService.deleteProjectById(id);
        return ResponseEntity.noContent().build();
    }
}
