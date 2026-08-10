package dev.eyadsharkawy.agency_os_api.global.workspace.service;

import static org.mockito.Mockito.*;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class TenantSchemaProvisioningServiceTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private DataSource dataSource;

  @InjectMocks private TenantSchemaProvisioningService provisioningService;

  @Test
  @DisplayName("createAndMigrateTenantSchema should execute CREATE SCHEMA and run Flyway migration")
  void createAndMigrateTenantSchema_Success() {
    String tenantId = "tenant_test_123";

    FluentConfiguration fluentConfiguration = mock(FluentConfiguration.class);
    Flyway flyway = mock(Flyway.class);

    try (MockedStatic<Flyway> flywayMockedStatic = mockStatic(Flyway.class)) {
      flywayMockedStatic.when(Flyway::configure).thenReturn(fluentConfiguration);
      when(fluentConfiguration.dataSource(dataSource)).thenReturn(fluentConfiguration);
      when(fluentConfiguration.schemas(tenantId)).thenReturn(fluentConfiguration);
      when(fluentConfiguration.locations("classpath:db/migration/tenant"))
          .thenReturn(fluentConfiguration);
      when(fluentConfiguration.load()).thenReturn(flyway);

      provisioningService.createAndMigrateTenantSchema(tenantId);

      verify(jdbcTemplate, times(1)).execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
      verify(flyway, times(1)).migrate();
    }
  }
}
