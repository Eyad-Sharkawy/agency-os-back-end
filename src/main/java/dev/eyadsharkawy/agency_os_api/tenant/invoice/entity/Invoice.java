package dev.eyadsharkawy.agency_os_api.tenant.invoice.entity;

import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    private void mapFromRequestWithClient(InvoiceRequest invoiceRequest, Client client) {
        this.client = client;
        status = invoiceRequest.status();
    }
}
