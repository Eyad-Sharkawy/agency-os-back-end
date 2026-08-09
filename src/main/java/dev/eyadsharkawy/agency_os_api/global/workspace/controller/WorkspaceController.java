package dev.eyadsharkawy.agency_os_api.global.workspace.controller;

import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WorkspaceRequest request) {

        WorkspaceResponse response = workspaceService.createWorkspace(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getUserWorkspaces(@AuthenticationPrincipal Jwt jwt) {
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(jwt);
        return ResponseEntity.ok(workspaces);
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable String tenantId,
            @Valid @RequestBody WorkspaceRequest request) {

        WorkspaceResponse response = workspaceService.updateUserWorkspaceByTenantId(tenantId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable String tenantId) {
        workspaceService.deleteUserWorkspaceByTenantId(tenantId);
        return ResponseEntity.noContent().build();
    }
}
