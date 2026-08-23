package dev.eyadsharkawy.agency_os_api.global.user.dto;

import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
    name = "03.0. UserProfileResponse",
    description = "Authenticated user profile details synchronized from Keycloak")
public record UserProfileResponse(
    @Schema(
            description = "Internal database ID of the user",
            example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,
    @Schema(
            description = "Keycloak subject identifier (UUID)",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        String keycloakId,
    @Schema(description = "Keycloak preferred username", example = "john_doe") String username,
    @Schema(description = "Primary email address", example = "john.doe@agency.com") String email,
    @Schema(description = "First name", example = "John") String firstName,
    @Schema(description = "Last name", example = "Doe") String lastName) {

  public static UserProfileResponse fromEntity(AppUser user) {
    return new UserProfileResponse(
        user.getId(),
        user.getKeycloakId(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName());
  }
}
