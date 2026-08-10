package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspace;

import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String role
) {
    public static WorkspaceMemberResponse fromEntity(UserWorkspace userWorkspace) {
        return new WorkspaceMemberResponse(
                userWorkspace.getUser().getId(),
                userWorkspace.getUser().getUsername(),
                userWorkspace.getUser().getEmail(),
                userWorkspace.getUser().getFirstName(),
                userWorkspace.getUser().getLastName(),
                userWorkspace.getRole().name()
        );
    }
}
