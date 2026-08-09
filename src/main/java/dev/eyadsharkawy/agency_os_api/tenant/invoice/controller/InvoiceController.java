package dev.eyadsharkawy.agency_os_api.tenant.invoice.controller;

import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceRequest;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceResponse;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> responses = invoiceService.getAllInvoices();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByClientId(@PathVariable UUID clientId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateInvoiceById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoiceById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
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
