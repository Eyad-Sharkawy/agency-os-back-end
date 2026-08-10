package dev.eyadsharkawy.agency_os_api.global.workspace.controller;

import dev.eyadsharkawy.agency_os_api.global.workspace.dto.*;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    // Only OWNER can update workspace details
    @PutMapping("/{tenantId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable String tenantId,
            @Valid @RequestBody WorkspaceRequest request) {

        WorkspaceResponse response = workspaceService.updateUserWorkspaceByTenantId(tenantId, request);
        return ResponseEntity.ok(response);
    }

    // Only OWNER can delete this workspace
    @DeleteMapping("/{tenantId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable String tenantId) {
        workspaceService.deleteUserWorkspaceByTenantId(tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantId}/members")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER', 'ADMIN')")
    public ResponseEntity<List<WorkspaceMemberResponse>> getWorkspaceMembers(@PathVariable String tenantId) {
        List<WorkspaceMemberResponse> members = workspaceService.getWorkspaceMembers(tenantId);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/{tenantId}/members/{userId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER', 'ADMIN')")
    public ResponseEntity<Void> updateWorkspaceMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String tenantId,
            @PathVariable UUID userId,
            @Valid @RequestBody WorkspaceMemberUpdateRequest request) {

        workspaceService.updateWorkspaceMember(jwt, tenantId, userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tenantId}/members/{userId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER', 'ADMIN')")
    public ResponseEntity<Void> removeWorkspaceMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String tenantId,
            @PathVariable UUID userId) {

        workspaceService.removeWorkspaceMember(jwt.getSubject(), tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/transfer-ownership")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> transferOwnership(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String tenantId,
            @Valid @RequestBody WorkspaceOwnershipTransferRequest request) {

        workspaceService.transferOwnership(jwt.getSubject(), tenantId, request.newOwnerId());
        return ResponseEntity.ok().build();
    }
}
