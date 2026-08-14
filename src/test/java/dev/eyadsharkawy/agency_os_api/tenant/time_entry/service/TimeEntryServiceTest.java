package dev.eyadsharkawy.agency_os_api.tenant.time_entry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private ActiveTimerRepository activeTimerRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private WorkspaceSecurity workspaceSecurity;

  @InjectMocks private TimeEntryService timeEntryService;

  private Jwt jwt;
  private Task task;
  private UUID taskId;

  @BeforeEach
  void setUp() {
    jwt = mock(Jwt.class);
    lenient().when(jwt.getSubject()).thenReturn("kc-user-123");

    taskId = UUID.randomUUID();
    task = new Task();
    task.setId(taskId);
    task.setTitle("Develop Feature");
    task.setAssigneeIds(Set.of("kc-user-123", "kc-target-456"));
  }

  @Test
  @DisplayName("logTimeManually should throw exception if user is not assigned to task")
  void logTimeManually_UserNotAssigned_ThrowsException() {
    task.setAssigneeIds(Set.of("other-user"));
    TimeEntryRequest request = new TimeEntryRequest(taskId, 60, true);

    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> timeEntryService.logTimeManually(jwt, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not assigned to");
  }

  @Test
  @DisplayName("logTimeManually should save time entry and return response")
  void logTimeManually_Success() {
    TimeEntryRequest request = new TimeEntryRequest(taskId, 60, true);

    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(timeEntryRepository.save(any(TimeEntry.class)))
        .thenAnswer(
            i -> {
              TimeEntry te = i.getArgument(0);
              te.setId(UUID.randomUUID());
              return te;
            });

    TimeEntryResponse response = timeEntryService.logTimeManually(jwt, request);

    assertThat(response).isNotNull();
    assertThat(response.durationMinutes()).isEqualTo(60);
    assertThat(response.isBillable()).isTrue();
    verify(timeEntryRepository, times(1)).save(any(TimeEntry.class));
  }

  @Test
  @DisplayName("logTimeManually on behalf of other user by OWNER or ADMIN should succeed")
  void logTimeManually_OnBehalf_OwnerAdmin_Success() {
    TimeEntryRequest request = new TimeEntryRequest(taskId, 90, true, "kc-target-456");

    when(workspaceSecurity.hasRole("OWNER", "ADMIN")).thenReturn(true);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(timeEntryRepository.save(any(TimeEntry.class)))
        .thenAnswer(
            i -> {
              TimeEntry te = i.getArgument(0);
              te.setId(UUID.randomUUID());
              return te;
            });

    TimeEntryResponse response = timeEntryService.logTimeManually(jwt, request);

    assertThat(response).isNotNull();
    assertThat(response.userId()).isEqualTo("kc-target-456");
    assertThat(response.durationMinutes()).isEqualTo(90);
    verify(timeEntryRepository, times(1)).save(any(TimeEntry.class));
  }

  @Test
  @DisplayName(
      "logTimeManually on behalf of other user by MEMBER should throw AccessDeniedException")
  void logTimeManually_OnBehalf_Member_ThrowsAccessDeniedException() {
    TimeEntryRequest request = new TimeEntryRequest(taskId, 90, true, "kc-target-456");

    when(workspaceSecurity.hasRole("OWNER", "ADMIN")).thenReturn(false);

    assertThatThrownBy(() -> timeEntryService.logTimeManually(jwt, request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only OWNER or ADMIN can log time on behalf of other team members");
  }

  @Test
  @DisplayName(
      "logTimeManually on behalf of other user who is not assigned should throw IllegalArgumentException")
  void logTimeManually_OnBehalf_TargetNotAssigned_ThrowsException() {
    task.setAssigneeIds(Set.of("kc-user-123")); // target "kc-target-456" not assigned
    TimeEntryRequest request = new TimeEntryRequest(taskId, 90, true, "kc-target-456");

    when(workspaceSecurity.hasRole("OWNER", "ADMIN")).thenReturn(true);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

    assertThatThrownBy(() -> timeEntryService.logTimeManually(jwt, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not assigned to");
  }

  @Test
  @DisplayName("startTimer should throw exception if active timer already exists")
  void startTimer_AlreadyRunning_ThrowsException() {
    when(activeTimerRepository.existsById("kc-user-123")).thenReturn(true);

    assertThatThrownBy(() -> timeEntryService.startTimer(jwt, taskId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already have a running timer");
  }

  @Test
  @DisplayName("startTimer should save ActiveTimer and return response")
  void startTimer_Success() {
    when(activeTimerRepository.existsById("kc-user-123")).thenReturn(false);
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(activeTimerRepository.save(any(ActiveTimer.class))).thenAnswer(i -> i.getArgument(0));

    ActiveTimerResponse response = timeEntryService.startTimer(jwt, taskId);

    assertThat(response).isNotNull();
    assertThat(response.userId()).isEqualTo("kc-user-123");
    verify(activeTimerRepository, times(1)).save(any(ActiveTimer.class));
  }

  @Test
  @DisplayName("stopTimer should throw ResourceNotFoundException if no active timer")
  void stopTimer_NoActiveTimer_ThrowsException() {
    when(activeTimerRepository.findById("kc-user-123")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> timeEntryService.stopTimer(jwt, true))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("No active timer found");
  }

  @Test
  @DisplayName("stopTimer should compute duration, save TimeEntry, and delete ActiveTimer")
  void stopTimer_Success() {
    ActiveTimer activeTimer = new ActiveTimer();
    activeTimer.setUserId("kc-user-123");
    activeTimer.setTask(task);
    activeTimer.setStartTime(Instant.now().minusSeconds(120)); // 2 mins ago

    when(activeTimerRepository.findById("kc-user-123")).thenReturn(Optional.of(activeTimer));
    when(timeEntryRepository.save(any(TimeEntry.class)))
        .thenAnswer(
            i -> {
              TimeEntry te = i.getArgument(0);
              te.setId(UUID.randomUUID());
              return te;
            });

    TimeEntryResponse response = timeEntryService.stopTimer(jwt, true);

    assertThat(response).isNotNull();
    assertThat(response.durationMinutes()).isGreaterThanOrEqualTo(2);
    verify(timeEntryRepository, times(1)).save(any(TimeEntry.class));
    verify(activeTimerRepository, times(1)).delete(activeTimer);
  }

  @Test
  @DisplayName("getActiveTimer should return Optional of ActiveTimerResponse")
  void getActiveTimer_Success() {
    ActiveTimer activeTimer = new ActiveTimer();
    activeTimer.setUserId("kc-user-123");
    activeTimer.setTask(task);
    activeTimer.setStartTime(Instant.now());

    when(activeTimerRepository.findById("kc-user-123")).thenReturn(Optional.of(activeTimer));

    Optional<ActiveTimerResponse> timerOpt = timeEntryService.getActiveTimer(jwt);

    assertThat(timerOpt).isPresent();
    assertThat(timerOpt.get().userId()).isEqualTo("kc-user-123");
  }

  @Test
  @DisplayName("deleteTimeEntry should delete entry when found")
  void deleteTimeEntry_Success() {
    UUID entryId = UUID.randomUUID();
    TimeEntry entry = new TimeEntry();
    entry.setId(entryId);

    when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

    timeEntryService.deleteTimeEntry(entryId);

    verify(timeEntryRepository, times(1)).delete(entry);
  }

  @Test
  @DisplayName("deleteTimeEntry should throw ResourceNotFoundException when entry not found")
  void deleteTimeEntry_NotFound() {
    UUID entryId = UUID.randomUUID();
    when(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> timeEntryService.deleteTimeEntry(entryId))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
