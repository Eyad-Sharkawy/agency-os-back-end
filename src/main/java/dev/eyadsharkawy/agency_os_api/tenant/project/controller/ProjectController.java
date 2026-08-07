package dev.eyadsharkawy.agency_os_api.tenant.project.controller;

import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectRequest;
import dev.eyadsharkawy.agency_os_api.tenant.project.dto.ProjectResponse;
import dev.eyadsharkawy.agency_os_api.tenant.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> responses = projectService.getAllProjects();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByClientId(@PathVariable UUID clientId) {
        List<ProjectResponse> responses = projectService.getProjectsByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.updateProjectById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProjectById(id);
        return ResponseEntity.noContent().build();
    }
}
