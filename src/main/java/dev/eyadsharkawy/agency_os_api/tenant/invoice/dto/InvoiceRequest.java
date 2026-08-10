package dev.eyadsharkawy.agency_os_api.tenant.invoice.dto;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(
    name = "07.1. InvoiceRequest",
    description = "Request payload for creating or updating an invoice")
public record InvoiceRequest(
    @Schema(
            description = "ID of the client for whom the invoice is created",
            example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "Client ID is required")
        UUID clientId,
    @Schema(description = "Status of the invoice", example = "DRAFT")
        @NotNull(message = "Invoice status is required")
        InvoiceStatus status) {}
