package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(
    name = "01.5. WorkspaceOwnershipTransferRequest",
    description = "Request payload for transferring workspace ownership to another member")
public record WorkspaceOwnershipTransferRequest(
    @Schema(
            description = "User ID of the member who will become the new owner",
            example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "New owner ID is required")
        UUID newOwnerId) {}
