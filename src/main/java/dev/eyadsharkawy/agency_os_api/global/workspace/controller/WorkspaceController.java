package dev.eyadsharkawy.agency_os_api.global.workspace.controller;

import dev.eyadsharkawy.agency_os_api.global.workspace.dto.*;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "01. Workspaces", description = "Endpoints for managing workspace organizations, membership listings, roles, and ownership transfers")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @Operation(summary = "Create workspace", description = "Initializes a new workspace tenant organization. The requesting user is automatically assigned the OWNER role.")
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WorkspaceRequest request) {

        WorkspaceResponse response = workspaceService.createWorkspace(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get user workspaces", description = "Retrieves all workspace organizations associated with the currently authenticated user.")
    public ResponseEntity<List<WorkspaceResponse>> getUserWorkspaces(@AuthenticationPrincipal Jwt jwt) {
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(jwt);
        return ResponseEntity.ok(workspaces);
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER')")
    @Operation(summary = "Update workspace settings", description = "Updates metadata (e.g. name or contact email) of the specified workspace. Restricted strictly to the OWNER.")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @Parameter(description = "The workspace tenant ID") @PathVariable String tenantId,
            @Valid @RequestBody WorkspaceRequest request) {

        WorkspaceResponse response = workspaceService.updateUserWorkspaceByTenantId(tenantId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tenantId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER')")
    @Operation(summary = "Delete workspace", description = "Performs a soft delete on a workspace tenant organization. Restricted strictly to the OWNER.")
    public ResponseEntity<Void> deleteWorkspace(
            @Parameter(description = "The workspace tenant ID") @PathVariable String tenantId) {
        workspaceService.deleteUserWorkspaceByTenantId(tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantId}/members")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER', 'ADMIN')")
    @Operation(summary = "List workspace members", description = "Retrieves the complete list of users registered in this workspace, alongside their active roles. Restricted to OWNER or ADMIN.")
    public ResponseEntity<List<WorkspaceMemberResponse>> getWorkspaceMembers(
            @Parameter(description = "The workspace tenant ID") @PathVariable String tenantId) {
        List<WorkspaceMemberResponse> members = workspaceService.getWorkspaceMembers(tenantId);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/{tenantId}/members/{userId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER', 'ADMIN')")
    @Operation(summary = "Update workspace member role", description = "Changes the role (ADMIN, MEMBER, CLIENT) of a team member. Admins are blocked from promoting other admins or modifying other admins/clients. Restricted to OWNER or ADMIN.")
    public ResponseEntity<Void> updateWorkspaceMember(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "The workspace tenant ID") @PathVariable String tenantId,
            @Parameter(description = "The target user ID to modify") @PathVariable UUID userId,
            @Valid @RequestBody WorkspaceMemberUpdateRequest request) {

        workspaceService.updateWorkspaceMember(jwt, tenantId, userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tenantId}/members/{userId}")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER', 'ADMIN')")
    @Operation(summary = "Remove member from workspace", description = "Removes a user from this workspace. Admins are blocked from removing other admins or clients. Restricted to OWNER or ADMIN.")
    public ResponseEntity<Void> removeWorkspaceMember(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "The workspace tenant ID") @PathVariable String tenantId,
            @Parameter(description = "The target user ID to remove") @PathVariable UUID userId) {

        workspaceService.removeWorkspaceMember(jwt.getSubject(), tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/transfer-ownership")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER')")
    @Operation(summary = "Transfer workspace ownership", description = "Transfers the OWNER status to a target member, demoting the current caller to ADMIN. Restricted strictly to the OWNER.")
    public ResponseEntity<Void> transferOwnership(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "The workspace tenant ID") @PathVariable String tenantId,
            @Valid @RequestBody WorkspaceOwnershipTransferRequest request) {

        workspaceService.transferOwnership(jwt.getSubject(), tenantId, request.newOwnerId());
        return ResponseEntity.ok().build();
    }
}
