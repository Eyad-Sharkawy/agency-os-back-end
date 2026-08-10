package dev.eyadsharkawy.agency_os_api.tenant.invoice.controller;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceRequest;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceResponse;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "07. Invoices", description = "Endpoints for auto-generating invoices, updating billing status, listing invoices, and downloading multi-page PDFs")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    @Operation(summary = "Create invoice", description = "Consolidates all unbilled time entries for a client and auto-generates a billing invoice. Restricted strictly to the OWNER.")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'CLIENT')")
    @Operation(summary = "List all invoices", description = "Retrieves all invoices. CLIENT portal contacts only see invoices associated with their company. MEMBERS are completely blocked. Restricted to OWNER, ADMIN, or CLIENT.")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> responses = invoiceService.getAllInvoices();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'CLIENT')")
    @Operation(summary = "Get invoice by ID", description = "Retrieves metadata of a specific invoice. CLIENT portal contacts can only look up their own company invoices. Restricted to OWNER, ADMIN, or CLIENT.")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @Parameter(description = "The invoice unique ID") @PathVariable UUID id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get invoices by Client ID", description = "Lists all invoices generated for a specific client company. Restricted to OWNER or ADMIN.")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByClientId(
            @Parameter(description = "The client company unique ID") @PathVariable UUID clientId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    @Operation(summary = "Update invoice", description = "Modifies an invoice's status or details. Restricted strictly to the OWNER.")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @Parameter(description = "The invoice unique ID") @PathVariable UUID id,
            @Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateInvoiceById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
    @Operation(summary = "Delete invoice", description = "Permanently deletes an invoice and returns all billed time entries back to 'unbilled' status. Restricted strictly to the OWNER.")
    public ResponseEntity<Void> deleteInvoice(
            @Parameter(description = "The invoice unique ID") @PathVariable UUID id) {
        invoiceService.deleteInvoiceById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'CLIENT')")
    @Operation(summary = "Download invoice PDF document", description = "Renders and outputs a print-ready multi-page PDF document containing breakdown metrics, logo, and payment instructions. CLIENT users can only download their own invoice PDFs.")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @Parameter(description = "The invoice unique ID") @PathVariable UUID id) {
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
