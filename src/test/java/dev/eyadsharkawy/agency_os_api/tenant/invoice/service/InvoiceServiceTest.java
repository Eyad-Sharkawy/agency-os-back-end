package dev.eyadsharkawy.agency_os_api.tenant.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.UserWorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientUserRepository;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceRequest;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.dto.InvoiceResponse;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.repository.InvoiceRepository;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository.TimeEntryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

  @Mock private InvoiceRepository invoiceRepository;
  @Mock private ClientRepository clientRepository;
  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private ClientUserRepository clientUserRepository;
  @Mock private UserWorkspaceRepository userWorkspaceRepository;

  @InjectMocks private InvoiceService invoiceService;

  private Client client;
  private Invoice invoice;
  private Project project;
  private Task task;
  private TimeEntry timeEntry;
  private UUID clientId;
  private UUID invoiceId;
  private Jwt jwt;

  @BeforeEach
  void setUp() {
    clientId = UUID.randomUUID();
    invoiceId = UUID.randomUUID();

    client = new Client();
    client.setId(clientId);
    client.setName("Globex Corp");
    client.setEmail("billing@globex.com");

    project = new Project();
    project.setId(UUID.randomUUID());
    project.setName("App Redesign");
    project.setClient(client);
    project.setBillingRate(new BigDecimal("100.00"));

    task = new Task();
    task.setId(UUID.randomUUID());
    task.setTitle("Setup Auth");
    task.setProject(project);

    timeEntry = new TimeEntry();
    timeEntry.setId(UUID.randomUUID());
    timeEntry.setTask(task);
    timeEntry.setDurationMinutes(120); // 2 hours
    timeEntry.setBillable(true);

    invoice = new Invoice();
    invoice.setId(invoiceId);
    invoice.setClient(client);
    invoice.setStatus(InvoiceStatus.DRAFT);
    invoice.setTotalAmount(new BigDecimal("200.00"));
    invoice.setCreatedAt(Instant.now());

    jwt = mock(Jwt.class);
    lenient().when(jwt.getSubject()).thenReturn("kc-user-123");

    TenantContextHolder.setTenantId("tenant_acme");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  private void mockSecurityContext(WorkspaceRole role) {
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(jwt);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(securityContext);

    if (role != null) {
      when(userWorkspaceRepository.findRoleByKeycloakIdAndTenantId("kc-user-123", "tenant_acme"))
          .thenReturn(Optional.of(role));
    }
  }

  @Test
  @DisplayName("createInvoice should throw exception when no unbilled time entries")
  void createInvoice_NoUnbilledEntries_ThrowsException() {
    InvoiceRequest request = new InvoiceRequest(clientId, InvoiceStatus.DRAFT);
    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
    when(timeEntryRepository.findUnbilledBillableEntriesByClientId(clientId)).thenReturn(List.of());

    assertThatThrownBy(() -> invoiceService.createInvoice(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No uninvoiced billable time entries found");
  }

  @Test
  @DisplayName("createInvoice should compute total cost and save invoice successfully")
  void createInvoice_Success() {
    InvoiceRequest request = new InvoiceRequest(clientId, InvoiceStatus.DRAFT);

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
    when(timeEntryRepository.findUnbilledBillableEntriesByClientId(clientId))
        .thenReturn(List.of(timeEntry));
    when(invoiceRepository.save(any(Invoice.class)))
        .thenAnswer(
            i -> {
              Invoice inv = i.getArgument(0);
              inv.setId(invoiceId);
              return inv;
            });

    InvoiceResponse response = invoiceService.createInvoice(request);

    assertThat(response).isNotNull();
    assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    verify(timeEntryRepository, times(1)).save(timeEntry);
    verify(invoiceRepository, times(1)).save(any(Invoice.class));
  }

  @Test
  @DisplayName("getAllInvoices for CLIENT role should filter by client user company")
  void getAllInvoices_ClientRole_Filtered() {
    mockSecurityContext(WorkspaceRole.CLIENT);

    ClientUser clientUser = new ClientUser();
    clientUser.setUserId("kc-user-123");
    clientUser.setClient(client);

    when(clientUserRepository.findById("kc-user-123")).thenReturn(Optional.of(clientUser));
    when(invoiceRepository.findByClientId(clientId)).thenReturn(List.of(invoice));

    List<InvoiceResponse> responses = invoiceService.getAllInvoices();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(invoiceId);
  }

  @Test
  @DisplayName(
      "getInvoiceById for CLIENT role should throw AccessDeniedException if client mismatch")
  void getInvoiceById_ClientMismatch_AccessDenied() {
    mockSecurityContext(WorkspaceRole.CLIENT);

    Client otherClient = new Client();
    otherClient.setId(UUID.randomUUID());
    ClientUser clientUser = new ClientUser();
    clientUser.setClient(otherClient);

    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(clientUserRepository.findById("kc-user-123")).thenReturn(Optional.of(clientUser));

    assertThatThrownBy(() -> invoiceService.getInvoiceById(invoiceId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("not authorized to view this invoice");
  }

  @Test
  @DisplayName("getInvoicesByClientId should return invoices for valid client")
  void getInvoicesByClientId_Success() {
    when(clientRepository.existsById(clientId)).thenReturn(true);
    when(invoiceRepository.findByClientId(clientId)).thenReturn(List.of(invoice));

    List<InvoiceResponse> responses = invoiceService.getInvoicesByClientId(clientId);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(invoiceId);
  }

  @Test
  @DisplayName("updateInvoiceById should update status and save invoice")
  void updateInvoiceById_Success() {
    InvoiceRequest request = new InvoiceRequest(clientId, InvoiceStatus.PAID);
    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

    InvoiceResponse response = invoiceService.updateInvoiceById(invoiceId, request);

    assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
    verify(invoiceRepository, times(1)).save(invoice);
  }

  @Test
  @DisplayName("deleteInvoiceById should delete invoice when found")
  void deleteInvoiceById_Success() {
    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

    invoiceService.deleteInvoiceById(invoiceId);

    verify(invoiceRepository, times(1)).delete(invoice);
  }

  @Test
  @DisplayName("generateInvoicePdf should generate PDF bytes for invoice")
  void generateInvoicePdf_Success() {
    Workspace ws = new Workspace();
    ws.setName("Acme Agency");
    ws.setContactEmail("contact@acme.com");

    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(workspaceRepository.findByTenantId("tenant_acme")).thenReturn(Optional.of(ws));
    when(timeEntryRepository.findByInvoiceId(invoiceId)).thenReturn(List.of(timeEntry));

    byte[] pdfBytes = invoiceService.generateInvoicePdf(invoiceId);

    assertThat(pdfBytes).isNotNull();
    assertThat(pdfBytes).hasSizeGreaterThan(0);
  }
}
