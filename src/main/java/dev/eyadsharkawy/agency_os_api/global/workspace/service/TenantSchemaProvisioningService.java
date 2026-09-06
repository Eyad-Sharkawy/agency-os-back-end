package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
  private final WorkspaceRepository workspaceRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void migrateAllExistingTenantSchemas() {
    log.info("Checking and migrating all existing tenant schemas on startup...");
    try {
      List<Workspace> workspaces = workspaceRepository.findAll();
      for (Workspace workspace : workspaces) {
        String tenantId = workspace.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
          log.info("Migrating schema for existing tenant: [{}]", tenantId);
          migrateTenantSchema(tenantId);
        }
      }
      log.info("Finished checking and migrating all existing tenant schemas.");
    } catch (Exception e) {
      log.error("Failed to migrate existing tenant schemas on application startup", e);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void createAndMigrateTenantSchema(String tenantId) {
    log.info("Provisioning database schema for new tenant: [{}]", tenantId);

    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
    migrateTenantSchema(tenantId);
    log.info("Successfully provisioned and migrated schema for tenant: [{}]", tenantId);
  }

  public void migrateTenantSchema(String tenantId) {
    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(tenantId)
            .locations("classpath:db/migration/tenant")
            .baselineOnMigrate(true)
            .load();

    flyway.migrate();
  }
}
