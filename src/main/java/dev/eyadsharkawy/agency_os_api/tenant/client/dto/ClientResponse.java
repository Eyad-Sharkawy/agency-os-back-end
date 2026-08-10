package dev.eyadsharkawy.agency_os_api.tenant.client.dto;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "03.2. ClientResponse", description = "Response details of a client")
public record ClientResponse(
        @Schema(description = "Unique identifier of the client", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Client name", example = "Globex Corporation")
        String name,

        @Schema(description = "Client contact email", example = "contact@globex.com")
        String email,

        @Schema(description = "Client status", example = "ACTIVE")
        ClientStatus status,

        @Schema(description = "Timestamp when client record was created", example = "2026-01-01T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when client record was last updated", example = "2026-01-02T12:00:00Z")
        Instant updatedAt
) {
    public static ClientResponse fromEntity(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getStatus(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
