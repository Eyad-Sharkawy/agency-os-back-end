package dev.eyadsharkawy.agency_os_api.tenant.invoice.controller;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceRequest;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceResponse;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'CLIENT')")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> responses = invoiceService.getAllInvoices();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'CLIENT')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    // Owners and Admins can query invoices by client company ID
    @GetMapping("/client/{clientId}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByClientId(@PathVariable UUID clientId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    // Only OWNER can update invoices
    @PutMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateInvoiceById(id, request);
        return ResponseEntity.ok(response);
    }

    // Only OWNER can delete invoices
    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoiceById(id);
        return ResponseEntity.noContent().build();
    }

    // Owners, Admins, and Clients can download the invoice PDF (with Client ID checks inside the service layer)
    @GetMapping("/{id}/pdf")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'CLIENT')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID id) {
        byte[] pdfBytes = invoiceService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline")
                .filename("invoice-" + id + ".pdf")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
