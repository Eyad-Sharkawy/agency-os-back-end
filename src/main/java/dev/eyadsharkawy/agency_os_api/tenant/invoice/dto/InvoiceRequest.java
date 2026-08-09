package dev.eyadsharkawy.agency_os_api.tenant.invoice.dto;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InvoiceRequest(
        @NotNull(message = "Client ID is required")
        UUID clientId,

        @NotNull(message = "Invoice status is required")
        InvoiceStatus status
) {
}
