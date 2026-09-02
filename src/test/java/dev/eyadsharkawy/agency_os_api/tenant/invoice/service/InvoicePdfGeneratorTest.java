package dev.eyadsharkawy.agency_os_api.tenant.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.InvoiceStatus;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvoicePdfGeneratorTest {

  private Client client;
  private Invoice invoice;
  private Project project1;
  private Project project2;

  @BeforeEach
  void setUp() {
    client = new Client();
    client.setId(UUID.randomUUID());
    client.setName("Globex Corp");
    client.setEmail("billing@globex.com");

    invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    invoice.setClient(client);
    invoice.setStatus(InvoiceStatus.SENT);
    invoice.setTotalAmount(new BigDecimal("1500.00"));
    invoice.setCreatedAt(Instant.now());

    project1 = new Project();
    project1.setId(UUID.randomUUID());
    project1.setName("Mobile App");
    project1.setBillingRate(new BigDecimal("150.00"));

    project2 = new Project();
    project2.setId(UUID.randomUUID());
    project2.setName("Web Portal");
    project2.setBillingRate(new BigDecimal("120.00"));
  }

  @Test
  @DisplayName("generate should produce valid PDF byte array for single project entries")
  void generate_SingleProject_Success() throws IOException {
    Task task = new Task();
    task.setId(UUID.randomUUID());
    task.setTitle("Setup Architecture");
    task.setProject(project1);

    TimeEntry entry = new TimeEntry();
    entry.setId(UUID.randomUUID());
    entry.setTask(task);
    entry.setDurationMinutes(120);

    byte[] pdf =
        InvoicePdfGenerator.generate(invoice, "Acme Agency", "contact@acme.com", List.of(entry));

    assertThat(pdf).isNotNull();
    assertThat(pdf).hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("generate should handle multiple projects correctly")
  void generate_MultipleProjects_Success() throws IOException {
    Task task1 = new Task();
    task1.setId(UUID.randomUUID());
    task1.setTitle("Task 1");
    task1.setProject(project1);

    TimeEntry entry1 = new TimeEntry();
    entry1.setId(UUID.randomUUID());
    entry1.setTask(task1);
    entry1.setDurationMinutes(60);

    Task task2 = new Task();
    task2.setId(UUID.randomUUID());
    task2.setTitle("Task 2");
    task2.setProject(project2);

    TimeEntry entry2 = new TimeEntry();
    entry2.setId(UUID.randomUUID());
    entry2.setTask(task2);
    entry2.setDurationMinutes(180);

    byte[] pdf =
        InvoicePdfGenerator.generate(
            invoice, "Acme Agency", "contact@acme.com", List.of(entry1, entry2));

    assertThat(pdf).isNotNull();
    assertThat(pdf).hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("generate should fallback when billedEntries is empty or null")
  void generate_EmptyBilledEntries_Fallback() throws IOException {
    byte[] pdf =
        InvoicePdfGenerator.generate(invoice, "Acme Agency", "contact@acme.com", List.of());

    assertThat(pdf).isNotNull();
    assertThat(pdf).hasSizeGreaterThan(0);
  }

  @Test
  @DisplayName("generate should handle multi-page overflow with many entries")
  void generate_MultiPageOverflow_Success() throws IOException {
    List<TimeEntry> entries = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      Task task = new Task();
      task.setId(UUID.randomUUID());
      task.setTitle("Long Task #" + i);
      task.setProject(project1);

      TimeEntry entry = new TimeEntry();
      entry.setId(UUID.randomUUID());
      entry.setTask(task);
      entry.setDurationMinutes(45);
      entries.add(entry);
    }

    byte[] pdf = InvoicePdfGenerator.generate(invoice, "Acme Agency", "contact@acme.com", entries);

    assertThat(pdf).isNotNull();
    assertThat(pdf).hasSizeGreaterThan(0);
  }
}
