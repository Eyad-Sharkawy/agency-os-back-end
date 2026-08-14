package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.eyadsharkawy.agency_os_api.BaseIntegrationTest;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantSchemaProvisioningIntegrationTest extends BaseIntegrationTest {

  @Autowired private TenantSchemaProvisioningService provisioningService;

  @Autowired private ClientRepository clientRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  private static final String TEST_TENANT = "tenant_integration_test";

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
    // Clean up created schema
    jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + TEST_TENANT + "\" CASCADE");
  }

  @Test
  @DisplayName(
      "Should dynamically provision schema, run migrations, and support JPA routing and auditing")
  void testSchemaProvisioningAndRouting() {
    // 1. Provision new tenant schema dynamically
    provisioningService.createAndMigrateTenantSchema(TEST_TENANT);

    // 2. Verify schema and one of the tables exists in Postgres catalog
    List<Map<String, Object>> schemas =
        jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?",
            TEST_TENANT);
    assertThat(schemas).hasSize(1);

    List<Map<String, Object>> tables =
        jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = 'clients'",
            TEST_TENANT);
    assertThat(tables).hasSize(1);

    // 3. Setup context and verify JPA routing works
    TenantContextHolder.setTenantId(TEST_TENANT);

    Client client = new Client();
    client.setName("Integration Test Client");
    client.setEmail("test@integration.com");
    client.setStatus(ClientStatus.ACTIVE);

    Client saved = clientRepository.save(client);

    assertThat(saved.getId()).isNotNull();
    // 4. Verify JPA Auditing is functioning (populated createdAt and updatedAt)
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    // 5. Query using raw JDBC to verify it is physically located in the tenant's schema
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            "SELECT name, email FROM \"" + TEST_TENANT + "\".clients WHERE id = ?", saved.getId());
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("name")).isEqualTo("Integration Test Client");
  }
}
