package dev.eyadsharkawy.agency_os_api.core.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import dev.eyadsharkawy.agency_os_api.BaseIntegrationTest;
import dev.eyadsharkawy.agency_os_api.global.workspace.service.TenantSchemaProvisioningService;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;
import dev.eyadsharkawy.agency_os_api.tenant.client.repository.ClientRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MultiTenantConcurrencyIntegrationTest extends BaseIntegrationTest {

  @Autowired private TenantSchemaProvisioningService provisioningService;

  @Autowired private ClientRepository clientRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  private static final String TENANT_A = "concurrency_tenant_a";
  private static final String TENANT_B = "concurrency_tenant_b";

  @BeforeEach
  void setUp() {
    provisioningService.createAndMigrateTenantSchema(TENANT_A);
    provisioningService.createAndMigrateTenantSchema(TENANT_B);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
    jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + TENANT_A + "\" CASCADE");
    jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + TENANT_B + "\" CASCADE");
  }

  @Test
  @DisplayName("Should maintain strict tenant data isolation under high concurrent load")
  void testConcurrentTenantAccess() throws InterruptedException, ExecutionException {
    int totalThreads = 40;
    ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    List<Future<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < totalThreads; i++) {
      final int index = i;
      final String tenantId = (index % 2 == 0) ? TENANT_A : TENANT_B;
      final String clientName = "Client_" + tenantId + "_" + index;

      futures.add(
          executor.submit(
              () -> {
                // Wait for all threads to start simultaneously
                startLatch.await();

                TenantContextHolder.setTenantId(tenantId);
                try {
                  // 1. Create client
                  Client client = new Client();
                  client.setName(clientName);
                  client.setEmail("test_" + index + "@concurrency.com");
                  client.setStatus(ClientStatus.ACTIVE);
                  clientRepository.save(client);

                  // 2. Query clients for this tenant
                  List<Client> clients = clientRepository.findAll();

                  // 3. Verify we ONLY see clients belonging to this tenant's schema
                  for (Client c : clients) {
                    if (tenantId.equals(TENANT_A)) {
                      assertThat(c.getName()).contains(TENANT_A);
                      assertThat(c.getName()).doesNotContain(TENANT_B);
                    } else {
                      assertThat(c.getName()).contains(TENANT_B);
                      assertThat(c.getName()).doesNotContain(TENANT_A);
                    }
                  }
                  return true;
                } finally {
                  TenantContextHolder.clear();
                }
              }));
    }

    // Release all threads at once to generate concurrent database connections and context queries
    startLatch.countDown();

    for (Future<Boolean> future : futures) {
      assertThat(future.get()).isTrue();
    }

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);

    // Verify final database states are isolated using raw JDBC
    TenantContextHolder.clear();
    Integer countA =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM \"" + TENANT_A + "\".clients", Integer.class);
    Integer countB =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM \"" + TENANT_B + "\".clients", Integer.class);

    assertThat(countA).isEqualTo(totalThreads / 2);
    assertThat(countB).isEqualTo(totalThreads / 2);
  }
}
