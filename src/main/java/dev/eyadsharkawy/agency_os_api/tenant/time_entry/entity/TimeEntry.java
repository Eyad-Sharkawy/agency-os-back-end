package dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity;

import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import dev.eyadsharkawy.agency_os_api.tenant.invoice.entity.Invoice;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "time_entries")
public class TimeEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "is_billable", nullable = false)
    private boolean isBillable = true;

    public void mapFromRequestWithIdAndTask(TimeEntryRequest request, String userId, Task task) {
        this.task = task;
        this.userId = userId;
        durationMinutes = request.durationMinutes();
        isBillable = request.isBillable();
    }
}
