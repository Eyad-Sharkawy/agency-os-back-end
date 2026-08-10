package dev.eyadsharkawy.agency_os_api.global.workspace.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WorkspaceOwnershipTransferRequest(
        @NotNull(message = "New owner ID is required")
        UUID newOwnerId
) {
}
