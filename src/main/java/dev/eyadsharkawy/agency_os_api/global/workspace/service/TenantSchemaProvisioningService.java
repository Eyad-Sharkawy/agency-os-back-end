package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSchemaProvisioningService {

  private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_]\\w*$");

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
          migrateTenantSafely(tenantId);
        }
      }
      log.info("Finished checking and migrating all existing tenant schemas.");
    } catch (Exception e) {
      log.error("Failed to migrate existing tenant schemas on application startup", e);
    }
  }

  private void migrateTenantSafely(String tenantId) {
    log.info("Migrating schema for existing tenant: [{}]", tenantId);
    try {
      migrateTenantSchema(tenantId);
    } catch (Exception ex) {
      log.error("Failed migrating schema for tenant: [{}]", tenantId, ex);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void createAndMigrateTenantSchema(String tenantId) {
    log.info("Provisioning database schema for new tenant: [{}]", tenantId);
    migrateTenantSchema(tenantId);
    log.info("Successfully provisioned and migrated schema for tenant: [{}]", tenantId);
  }

  public void migrateTenantSchema(String tenantId) {
    String validatedTenantId = validateTenantId(tenantId);
    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(validatedTenantId)
            .defaultSchema(validatedTenantId)
            .createSchemas(true)
            .locations("classpath:db/migration/tenant")
            .baselineOnMigrate(true)
            .validateOnMigrate(false)
            .outOfOrder(true)
            .load();

    flyway.migrate();
  }

  private String validateTenantId(String tenantId) {
    if (tenantId == null || !SAFE_IDENTIFIER.matcher(tenantId).matches()) {
      throw new IllegalArgumentException("Invalid tenant identifier: " + tenantId);
    }
    return tenantId;
  }
}
