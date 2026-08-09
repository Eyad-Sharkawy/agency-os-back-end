package dev.eyadsharkawy.agency_os_api.tenant.invoice.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceRequest;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceResponse;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.repository.InvoiceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TimeEntryRepository timeEntryRepository;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        log.info("Auto-generating invoice for client [{}]", request.clientId());

        Client client = findClientByIdOrThrow(request.clientId());

        List<TimeEntry> unbilledEntries = timeEntryRepository.findUnbilledBillableEntriesByClientId(request.clientId());

        if (unbilledEntries.isEmpty()) {
            throw new IllegalArgumentException("No uninvoiced billable time entries found for client: " + client.getName());
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (TimeEntry entry : unbilledEntries) {
            BigDecimal hours = BigDecimal.valueOf(entry.getDurationMinutes())
                    .divide(BigDecimal.valueOf(60.0), 4, RoundingMode.HALF_UP);
            BigDecimal billingRate = entry.getTask().getProject().getBillingRate();
            BigDecimal entryCost = hours.multiply(billingRate);

            totalAmount = totalAmount.add(entryCost);
        }

        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setStatus(request.status());
        invoice.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        Invoice savedInvoice = invoiceRepository.save(invoice);

        for (TimeEntry entry : unbilledEntries) {
            entry.setInvoice(savedInvoice);
            timeEntryRepository.save(entry);
        }

        return InvoiceResponse.fromEntity(savedInvoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        log.info("Fetching all invoices");
        return invoiceRepository.findAll().stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        log.info("Fetching invoice with id: {}", id);
        Invoice invoice = findInvoiceByIdOrThrow(id);
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByClientId(UUID clientId) {
        log.info("Fetching invoices for client: {}", clientId);
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with id: " + clientId);
        }
        return invoiceRepository.findByClientId(clientId).stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    @Transactional
    public InvoiceResponse updateInvoiceById(UUID id, InvoiceRequest request) {
        log.info("Updating invoice status/client for id: {}", id);
        Invoice invoice = findInvoiceByIdOrThrow(id);

        Client client = invoice.getClient();

        if (!client.getId().equals(request.clientId())) {
            client = findClientByIdOrThrow(request.clientId());
        }

        invoice.setClient(client);
        invoice.setStatus(request.status());
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(updatedInvoice);
    }

    @Transactional
    public void deleteInvoiceById(UUID id) {
        log.info("Deleting invoice with id: {}", id);
        Invoice invoice = findInvoiceByIdOrThrow(id);
        invoiceRepository.delete(invoice);
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(UUID id) {
        log.info("Generating PDF for invoice: {}", id);
        Invoice invoice = findInvoiceByIdOrThrow(id);

        // Fetch Workspace details
        String tenantId = TenantContextHolder.getTenantId();
        var workspaceOpt = workspaceRepository.findByTenantId(tenantId);
        String agencyName = workspaceOpt.map(ws -> ws.getName()).orElse("Agency OS Partner");
        String contactEmail = workspaceOpt.map(ws -> ws.getContactEmail()).orElse("billing@agency.com");

        // Fetch all time entries linked to this invoice
        List<TimeEntry> billedEntries = timeEntryRepository.findByInvoiceId(invoice.getId());

        try {
            return InvoicePdfGenerator.generate(invoice, agencyName, contactEmail, billedEntries);
        } catch (IOException e) {
            log.error("Failed to generate PDF invoice", e);
            throw new RuntimeException("Error rendering PDF", e);
        }
    }

    private Invoice findInvoiceByIdOrThrow(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    private Client findClientByIdOrThrow(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
    }
}
