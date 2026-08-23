package dev.eyadsharkawy.agency_os_api.global.user.controller;

import dev.eyadsharkawy.agency_os_api.global.user.dto.UserProfileResponse;
import dev.eyadsharkawy.agency_os_api.global.user.entity.AppUser;
import dev.eyadsharkawy.agency_os_api.global.user.service.UserSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "03. Users", description = "User profile and synchronization management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserSyncService userSyncService;

  @Operation(
      summary = "Get current authenticated user profile",
      description =
          "Retrieves the synchronized user profile for the authenticated Keycloak account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
      })
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    AppUser user = userSyncService.getOrSyncUser(jwt);
    return ResponseEntity.ok(UserProfileResponse.fromEntity(user));
  }
}
