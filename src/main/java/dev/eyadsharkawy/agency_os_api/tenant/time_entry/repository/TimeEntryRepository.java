package dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository;

import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {
    List<TimeEntry> findByTaskId(UUID taskId);

    List<TimeEntry> findByUserId(String userId);

    @Query("SELECT COALESCE(SUM(t.durationMinutes), 0) FROM TimeEntry t WHERE t.task.id = :taskId")
    int sumDurationMinutesByTaskId(@Param("taskId") UUID taskId);

    @Query("SELECT t FROM TimeEntry t WHERE t.task.project.client.id = :clientId AND t.isBillable = true AND t.invoice IS NULL")
    List<TimeEntry> findUnbilledBillableEntriesByClientId(@Param("clientId") UUID clientId);

    List<TimeEntry> findByInvoiceId(UUID invoiceId);
}
