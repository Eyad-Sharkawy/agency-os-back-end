package dev.eyadsharkawy.agency_os_api.global.workspace.controller;

import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationRequest;
import dev.eyadsharkawy.agency_os_api.global.workspace.dto.WorkspaceInvitationResponse;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.WorkspaceInvitationService;
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
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;

    @PostMapping("/{tenantId}/invitations")
    @PreAuthorize("@workspaceSecurity.hasRole(#tenantId, 'OWNER', 'ADMIN')")
    public ResponseEntity<WorkspaceInvitationResponse> inviteUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String tenantId,
            @Valid @RequestBody WorkspaceInvitationRequest request) {

        WorkspaceInvitationResponse response = invitationService.inviteUser(jwt, tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<WorkspaceInvitationResponse>> getPendingInvitations(
            @AuthenticationPrincipal Jwt jwt) {

        List<WorkspaceInvitationResponse> response = invitationService.getPendingInvitations(jwt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invitationId) {

        invitationService.acceptInvitation(jwt, invitationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public ResponseEntity<Void> declineInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invitationId) {

        invitationService.declineInvitation(jwt, invitationId);
        return ResponseEntity.ok().build();
    }
}
