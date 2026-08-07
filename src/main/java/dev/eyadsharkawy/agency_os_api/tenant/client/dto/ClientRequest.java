package dev.eyadsharkawy.agency_os_api.tenant.client.dto;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        @NotBlank(message = "Client name is required")
        String name,

        @Email(message = "Invalid Email")
        String email,

        @NotNull(message = "Client status is required")
        ClientStatus status
) {
}
