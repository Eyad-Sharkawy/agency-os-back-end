package dev.eyadsharkawy.agency_os_api.tenant.time_entry.service;

import dev.eyadsharkawy.agency_os_api.core.exceptions.ResourceNotFoundException;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import dev.eyadsharkawy.agency_os_api.tenant.task.repository.TaskRepository;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.ActiveTimerResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryRequest;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.ActiveTimer;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.TimeEntry;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository.ActiveTimerRepository;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository.TimeEntryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeEntryService {
  private static final String NO_ACTIVE_TIMER_MESSAGE = "No active timer found for this user";

  private final TimeEntryRepository timeEntryRepository;
  private final ActiveTimerRepository activeTimerRepository;
  private final TaskRepository taskRepository;
  private final WorkspaceSecurity workspaceSecurity;

  @Transactional
  public TimeEntryResponse logTimeManually(Jwt jwt, TimeEntryRequest request) {
    String callerUserId = jwt.getSubject();
    String targetUserId =
        (request.userId() != null && !request.userId().isBlank()) ? request.userId() : callerUserId;

    if (!callerUserId.equals(targetUserId) && !workspaceSecurity.hasRole("OWNER", "ADMIN")) {
      throw new AccessDeniedException(
          "Access Denied: Only OWNER or ADMIN can log time on behalf of other team members.");
    }

    log.info(
        "Caller [{}] manually logging [{}] minutes on task [{}] for target user [{}]",
        callerUserId,
        request.durationMinutes(),
        request.taskId(),
        targetUserId);

    Task task = findTaskByIdOrThrow(request.taskId());
    validateUserIsAssignedToTask(callerUserId, targetUserId, task);

    TimeEntry timeEntry = new TimeEntry();
    timeEntry.mapFromRequestWithIdAndTask(request, targetUserId, task);

    TimeEntry savedEntry = timeEntryRepository.save(timeEntry);
    return TimeEntryResponse.fromEntity(savedEntry);
  }

  @Transactional
  public ActiveTimerResponse startTimer(Jwt jwt, UUID taskId) {
    String userId = jwt.getSubject();
    log.info("User [{}] starting timer on task [{}]", userId, taskId);

    if (activeTimerRepository.existsById(userId)) {
      throw new IllegalArgumentException("You already have a running timer. Stop it first!");
    }

    Task task = findTaskByIdOrThrow(taskId);
    validateUserIsAssignedToTask(userId, userId, task);

    ActiveTimer activeTimer = new ActiveTimer();
    activeTimer.setUserId(userId);
    activeTimer.setTask(task);
    activeTimer.setStartTime(Instant.now());
    activeTimer.setPaused(false);
    activeTimer.setAccumulatedSeconds(0);
    activeTimer.setLastResumeTimestamp(Instant.now());

    ActiveTimer savedActiveTimer = activeTimerRepository.save(activeTimer);
    return ActiveTimerResponse.fromEntity(savedActiveTimer);
  }

  @Transactional
  public ActiveTimerResponse pauseTimer(Jwt jwt) {
    String userId = jwt.getSubject();
    log.info("User [{}] pausing active timer", userId);

    ActiveTimer activeTimer =
        activeTimerRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(NO_ACTIVE_TIMER_MESSAGE));

    if (!activeTimer.isPaused()) {
      Instant lastResume =
          activeTimer.getLastResumeTimestamp() != null
              ? activeTimer.getLastResumeTimestamp()
              : activeTimer.getStartTime();
      int elapsedSeconds =
          (int) Math.max(0, Duration.between(lastResume, Instant.now()).toSeconds());
      activeTimer.setAccumulatedSeconds(activeTimer.getAccumulatedSeconds() + elapsedSeconds);
      activeTimer.setPaused(true);
      activeTimer.setLastResumeTimestamp(null);
      activeTimer = activeTimerRepository.save(activeTimer);
    }

    return ActiveTimerResponse.fromEntity(activeTimer);
  }

  @Transactional
  public ActiveTimerResponse resumeTimer(Jwt jwt) {
    String userId = jwt.getSubject();
    log.info("User [{}] resuming active timer", userId);

    ActiveTimer activeTimer =
        activeTimerRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(NO_ACTIVE_TIMER_MESSAGE));

    if (activeTimer.isPaused()) {
      activeTimer.setPaused(false);
      activeTimer.setLastResumeTimestamp(Instant.now());
      activeTimer = activeTimerRepository.save(activeTimer);
    }

    return ActiveTimerResponse.fromEntity(activeTimer);
  }

  @Transactional
  public TimeEntryResponse stopTimer(Jwt jwt, boolean isBillable, Integer customDurationMinutes) {
    return stopTimerInternal(jwt, isBillable, customDurationMinutes);
  }

  @Transactional
  public TimeEntryResponse stopTimer(Jwt jwt, boolean isBillable) {
    return stopTimerInternal(jwt, isBillable, null);
  }

  private TimeEntryResponse stopTimerInternal(
      Jwt jwt, boolean isBillable, Integer customDurationMinutes) {
    String userId = jwt.getSubject();
    log.info("User [{}] stopping active timer", userId);

    ActiveTimer activeTimer =
        activeTimerRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(NO_ACTIVE_TIMER_MESSAGE));

    int durationMinutes;
    if (customDurationMinutes != null && customDurationMinutes > 0) {
      durationMinutes = customDurationMinutes;
    } else {
      int totalSeconds = activeTimer.getAccumulatedSeconds();
      if (!activeTimer.isPaused()) {
        Instant lastResume =
            activeTimer.getLastResumeTimestamp() != null
                ? activeTimer.getLastResumeTimestamp()
                : activeTimer.getStartTime();
        totalSeconds += (int) Math.max(0, Duration.between(lastResume, Instant.now()).toSeconds());
      }
      durationMinutes = Math.max(1, (int) Math.round(totalSeconds / 60.0));
    }

    TimeEntry timeEntry = new TimeEntry();
    timeEntry.setTask(activeTimer.getTask());
    timeEntry.setUserId(userId);
    timeEntry.setDurationMinutes(durationMinutes);
    timeEntry.setBillable(isBillable);

    TimeEntry savedTimeEnty = timeEntryRepository.save(timeEntry);

    activeTimerRepository.delete(activeTimer);

    return TimeEntryResponse.fromEntity(savedTimeEnty);
  }

  @Transactional(readOnly = true)
  public Optional<ActiveTimerResponse> getActiveTimer(Jwt jwt) {
    String userId = jwt.getSubject();
    if (userId == null) {
      return Optional.empty();
    }
    return activeTimerRepository.findById(userId).map(ActiveTimerResponse::fromEntity);
  }

  @Transactional(readOnly = true)
  public List<TimeEntryResponse> getTimeEntries(UUID taskId, String userId) {
    if (taskId != null) {
      return getTimeEntriesByTaskIdInternal(taskId);
    }
    if (userId != null && !userId.isBlank()) {
      return getTimeEntriesByUserIdInternal(userId);
    }
    log.info("Fetching all time entries for current tenant workspace");
    return timeEntryRepository.findAll().stream().map(TimeEntryResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public List<TimeEntryResponse> getTimeEntriesByTaskId(UUID taskId) {
    return getTimeEntriesByTaskIdInternal(taskId);
  }

  @Transactional(readOnly = true)
  public List<TimeEntryResponse> getTimeEntriesByUserId(String userId) {
    return getTimeEntriesByUserIdInternal(userId);
  }

  private List<TimeEntryResponse> getTimeEntriesByTaskIdInternal(UUID taskId) {
    log.info("Fetching time entries for task: {}", taskId);
    return timeEntryRepository.findByTaskId(taskId).stream()
        .map(TimeEntryResponse::fromEntity)
        .toList();
  }

  private List<TimeEntryResponse> getTimeEntriesByUserIdInternal(String userId) {
    log.info("Fetching time entries for user: {}", userId);
    return timeEntryRepository.findByUserId(userId).stream()
        .map(TimeEntryResponse::fromEntity)
        .toList();
  }

  @Transactional
  public void deleteTimeEntry(UUID id) {
    log.info("Deleting time entry: {}", id);
    TimeEntry entry =
        timeEntryRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Time entry not found with id: " + id));
    timeEntryRepository.delete(entry);
  }

  private Task findTaskByIdOrThrow(UUID taskId) {
    return taskRepository
        .findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
  }

  private void validateUserIsAssignedToTask(String callerUserId, String targetUserId, Task task) {
    if (callerUserId.equals(targetUserId) && workspaceSecurity.hasRole("OWNER", "ADMIN")) {
      return;
    }
    if (task.getAssigneeIds() == null || !task.getAssigneeIds().contains(targetUserId)) {
      throw new IllegalArgumentException("User is not assigned to this task.");
    }
  }
}
