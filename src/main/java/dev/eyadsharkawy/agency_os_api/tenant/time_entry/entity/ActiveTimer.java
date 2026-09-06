package dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "active_timers")
public class ActiveTimer {
  @Id
  @Column(name = "user_id", nullable = false)
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  private Task task;

  @Column(name = "start_time", nullable = false)
  private Instant startTime = Instant.now();

  @Column(name = "is_paused", nullable = false)
  private boolean isPaused = false;

  @Column(name = "accumulated_seconds", nullable = false)
  private int accumulatedSeconds = 0;

  @Column(name = "last_resume_timestamp")
  private Instant lastResumeTimestamp;
}
