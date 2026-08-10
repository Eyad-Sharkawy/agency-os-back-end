package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspace;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "01.3. WorkspaceMemberResponse", description = "Details of a member within a workspace")
public record WorkspaceMemberResponse(
        @Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,

        @Schema(description = "Username of the member", example = "john_doe")
        String username,

        @Schema(description = "Email address of the member", example = "john@example.com")
        String email,

        @Schema(description = "First name of the member", example = "John")
        String firstName,

        @Schema(description = "Last name of the member", example = "Doe")
        String lastName,

        @Schema(description = "Role of the member in the workspace", example = "ADMIN")
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
