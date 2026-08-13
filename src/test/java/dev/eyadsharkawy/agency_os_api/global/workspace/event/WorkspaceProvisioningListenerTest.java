package dev.eyadsharkawy.agency_os_api.global.workspace.event;

import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.global.workspace.service.TenantSchemaProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceProvisioningListenerTest {

  @Mock private TenantSchemaProvisioningService schemaService;

  private WorkspaceProvisioningListener listener;

  @BeforeEach
  void setUp() {
    listener = new WorkspaceProvisioningListener(schemaService);
  }

  @Test
  void onWorkspaceCreated_Success() {
    WorkspaceCreatedEvent event = new WorkspaceCreatedEvent("tenant-123");

    listener.onWorkspaceCreated(event);

    verify(schemaService, times(1)).createAndMigrateTenantSchema("tenant-123");
  }

  @Test
  void onWorkspaceCreated_Exception_Logged() {
    WorkspaceCreatedEvent event = new WorkspaceCreatedEvent("tenant-123");
    doThrow(new RuntimeException("Test DB Error"))
        .when(schemaService)
        .createAndMigrateTenantSchema(anyString());

    // Should not rethrow exception, just log it
    listener.onWorkspaceCreated(event);

    verify(schemaService, times(1)).createAndMigrateTenantSchema("tenant-123");
  }
}
