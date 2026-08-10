package dev.eyadsharkawy.agency_os_api.tenant.client.dto;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(
    name = "03.1. ClientRequest",
    description = "Request payload for creating or updating a client")
public record ClientRequest(
    @Schema(description = "Client company or individual name", example = "Globex Corporation")
        @NotBlank(message = "Client name is required")
        String name,
    @Schema(description = "Contact email address for the client", example = "contact@globex.com")
        @Email(message = "Invalid Email")
        String email,
    @Schema(description = "Status of the client relationship", example = "ACTIVE")
        @NotNull(message = "Client status is required")
        ClientStatus status) {}
