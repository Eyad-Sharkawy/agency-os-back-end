package dev.eyadsharkawy.agency_os_api.tenant.time_entry.controller;

import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.shared.service.WebSocketBroadcastService;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.ActiveTimerResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryRequest;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/time-entries")
@RequiredArgsConstructor
@PreAuthorize(
    "@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER')") // Lock out CLIENT portal users
// entirely
@Tag(
    name = "06. Time Tracking",
    description =
        "Endpoints for manual time logging, start/stop stopwatch timers, and WebSocket broadcast integrations")
public class TimeEntryController {

  private static final String TOPIC_PREFIX = "/topic/";

  private final TimeEntryService timeEntryService;
  private final WebSocketBroadcastService broadcastService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Log time manually",
      description =
          "Allows internal users to log a manual time entry for a task. Broadcasts updates to the active tenant channel via WebSockets.")
  public ResponseEntity<TimeEntryResponse> logTimeManually(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TimeEntryRequest request) {
    TimeEntryResponse response = timeEntryService.logTimeManually(jwt, request);

    String tenantId = TenantContextHolder.getTenantId();
    broadcastService.broadcast(TOPIC_PREFIX + tenantId + "/time-entries", response);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/start/{taskId}")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Start stopwatch timer",
      description =
          "Initiates a live stopwatch timer on a specific task. If another timer is running, it will automatically stop it first.")
  public ResponseEntity<ActiveTimerResponse> startTimer(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "The target task ID") @PathVariable UUID taskId) {
    ActiveTimerResponse response = timeEntryService.startTimer(jwt, taskId);

    String tenantId = TenantContextHolder.getTenantId();
    broadcastService.broadcast(TOPIC_PREFIX + tenantId + "/timers/start", response);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/stop")
  @Operation(
      summary = "Stop stopwatch timer",
      description =
          "Stops the currently running stopwatch timer for the user, converting the active duration into a logged time entry.")
  public ResponseEntity<TimeEntryResponse> stopTimer(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "Whether this tracked entry is billable to the client")
          @RequestParam(defaultValue = "true")
          boolean isBillable) {
    TimeEntryResponse response = timeEntryService.stopTimer(jwt, isBillable);

    String tenantId = TenantContextHolder.getTenantId();
    broadcastService.broadcast(TOPIC_PREFIX + tenantId + "/timers/stop", response);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/active")
  @Operation(
      summary = "Get user's active timer",
      description =
          "Retrieves the active running stopwatch timer metadata for the authenticated user, or returns 204 No Content if none is running.")
  public ResponseEntity<ActiveTimerResponse> getActiveTimer(@AuthenticationPrincipal Jwt jwt) {
    return timeEntryService
        .getActiveTimer(jwt)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping("/task/{taskId}")
  @Operation(
      summary = "Get time entries by Task",
      description =
          "Retrieves the list of all logged time entries associated with a specific task.")
  public ResponseEntity<List<TimeEntryResponse>> getTimeEntriesByTaskId(
      @Parameter(description = "The task ID") @PathVariable UUID taskId) {
    List<TimeEntryResponse> responses = timeEntryService.getTimeEntriesByTaskId(taskId);
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get time entries by User",
      description =
          "Retrieves the list of all logged time entries submitted by a specific user ID.")
  public ResponseEntity<List<TimeEntryResponse>> getTimeEntriesByUserId(
      @Parameter(description = "The target user's Keycloak ID") @PathVariable String userId) {
    List<TimeEntryResponse> responses = timeEntryService.getTimeEntriesByUserId(userId);
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Delete time entry",
      description = "Deletes a logged time entry from the timesheet history.")
  public ResponseEntity<Void> deleteTimeEntry(
      @Parameter(description = "The time entry unique ID") @PathVariable UUID id) {
    timeEntryService.deleteTimeEntry(id);
    return ResponseEntity.noContent().build();
  }
}
