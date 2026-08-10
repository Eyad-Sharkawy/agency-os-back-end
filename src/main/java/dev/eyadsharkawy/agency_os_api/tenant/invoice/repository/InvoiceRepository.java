package dev.eyadsharkawy.agency_os_api.tenant.invoice.repository;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
  List<Invoice> findByClientId(UUID clientId);
}
