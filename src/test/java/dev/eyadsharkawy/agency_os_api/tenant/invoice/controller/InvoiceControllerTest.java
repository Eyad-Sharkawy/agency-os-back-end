package dev.eyadsharkawy.agency_os_api.tenant.invoice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceRequest;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceResponse;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.service.InvoiceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvoiceController.class)
@Import(JacksonConfig.class)
class InvoiceControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private InvoiceService invoiceService;

  @MockitoBean(name = "workspaceSecurity")
  private WorkspaceSecurity workspaceSecurity;

  @MockitoBean private TenantSecurityFilter tenantSecurityFilter;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws Exception {
    Mockito.doAnswer(
            invocation -> {
              jakarta.servlet.ServletRequest request = invocation.getArgument(0);
              jakarta.servlet.ServletResponse response = invocation.getArgument(1);
              jakarta.servlet.FilterChain chain = invocation.getArgument(2);
              chain.doFilter(request, response);
              return null;
            })
        .when(tenantSecurityFilter)
        .doFilter(any(), any(), any());
  }

  @Test
  void testCreateInvoice_Success() throws Exception {
    InvoiceRequest request = new InvoiceRequest(UUID.randomUUID(), InvoiceStatus.DRAFT);
    InvoiceResponse response =
        new InvoiceResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            InvoiceStatus.DRAFT,
            Instant.now(),
            Instant.now());

    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(invoiceService.createInvoice(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/invoices")
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void testGetAllInvoices_Success() throws Exception {
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(invoiceService.getAllInvoices()).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/invoices")
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testGetInvoiceById_Success() throws Exception {
    UUID invoiceId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(invoiceService.getInvoiceById(invoiceId))
        .thenReturn(
            new InvoiceResponse(
                invoiceId,
                UUID.randomUUID(),
                BigDecimal.valueOf(100),
                InvoiceStatus.DRAFT,
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(
            get("/api/v1/invoices/{id}", invoiceId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testGetInvoicesByClientId_Success() throws Exception {
    UUID clientId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(invoiceService.getInvoicesByClientId(clientId)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/invoices/client/{clientId}", clientId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk());
  }

  @Test
  void testUpdateInvoice_Success() throws Exception {
    UUID invoiceId = UUID.randomUUID();
    InvoiceRequest request = new InvoiceRequest(UUID.randomUUID(), InvoiceStatus.SENT);

    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(invoiceService.updateInvoiceById(any(), any()))
        .thenReturn(
            new InvoiceResponse(
                invoiceId,
                UUID.randomUUID(),
                BigDecimal.valueOf(100),
                InvoiceStatus.SENT,
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(
            put("/api/v1/invoices/{id}", invoiceId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void testDeleteInvoice_Success() throws Exception {
    UUID invoiceId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);

    mockMvc
        .perform(
            delete("/api/v1/invoices/{id}", invoiceId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isNoContent());
  }

  @Test
  void testDownloadInvoicePdf_Success() throws Exception {
    UUID invoiceId = UUID.randomUUID();
    when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
    when(invoiceService.generateInvoicePdf(invoiceId)).thenReturn(new byte[] {1, 2, 3});

    mockMvc
        .perform(
            get("/api/v1/invoices/{id}/pdf", invoiceId)
                .header("X-Tenant-ID", "tenant1")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .contentType(MediaType.APPLICATION_PDF));
  }
}
