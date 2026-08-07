package dev.eyadsharkawy.agency_os_api.tenant.client.dto;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String email,
        ClientStatus status,
        Instant createdAt,
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
