package dev.eyadsharkawy.agency_os_api.tenant.task.entity;

import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import dev.eyadsharkawy.agency_os_api.tenant.task.dto.TaskRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ElementCollection
    @CollectionTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id")
    )
    @Column(name = "user_id")
    private Set<String> assigneeIds = new HashSet<>();

    public void mapFromRequestWithProject(TaskRequest request, Project project) {
        title = request.title();
        description = request.description();
        startDate = request.startDate();
        dueDate = request.dueDate();
        estimatedMinutes = request.estimatedMinutes();
        priority = request.priority();
        status = request.status();
        this.project = project;
        assigneeIds.clear();
        assigneeIds.addAll(request.assigneeIds());
    }
}
