package dev.eyadsharkawy.agency_os_api.tenant.invoice.dto;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "07.2. InvoiceResponse", description = "Response details of an invoice")
public record InvoiceResponse(
        @Schema(description = "Unique identifier of the invoice", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "ID of the client receiving the invoice", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID clientId,

        @Schema(description = "Total amount of the invoice", example = "1250.00")
        BigDecimal totalAmount,

        @Schema(description = "Current status of the invoice", example = "SENT")
        InvoiceStatus status,

        @Schema(description = "Timestamp when the invoice was created", example = "2026-01-01T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when the invoice was last updated", example = "2026-01-02T12:00:00Z")
        Instant updatedAt
) {

    public static InvoiceResponse fromEntity(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getClient().getId(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}
