package dev.eyadsharkawy.agency_os_api.tenant.invoice.dto;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID clientId,
        BigDecimal totalAmount,
        InvoiceStatus status,
        Instant createdAt,
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
