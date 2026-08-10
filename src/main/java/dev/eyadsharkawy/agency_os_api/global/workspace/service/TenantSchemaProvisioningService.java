package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSchemaProvisioningService {
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void createAndMigrateTenantSchema(String tenantId) {
    log.info("Provisioning database schema for new tenant: [{}]", tenantId);

    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");

    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(tenantId)
            .locations("classpath:db/migration/tenant")
            .load();

    flyway.migrate();
    log.info("Successfully provisioned and migrated schema for tenant: [{}]", tenantId);
  }
}
