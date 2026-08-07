package dev.eyadsharkawy.agency_os_api.global.workspace.event;

import dev.eyadsharkawy.agency_os_api.global.workspace.service.TenantSchemaProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceProvisioningListener {

    private final TenantSchemaProvisioningService schemaService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkspaceCreated(WorkspaceCreatedEvent event) {
        try {
            schemaService.createAndMigrateTenantSchema(event.tenantId());
        } catch (Exception e) {
            log.error("Schema provisioning failed for tenant [{}] — workspace row exists without a backing schema", event.tenantId(), e);
        }
    }
}
