package dev.eyadsharkawy.agency_os_api.tenant.time_entry.controller;

import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantContextHolder;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.ActiveTimerResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryRequest;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.service.TimeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {
    private final TimeEntryService timeEntryService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<TimeEntryResponse> logTimeManually(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TimeEntryRequest request
    ) {
        TimeEntryResponse response = timeEntryService.logTimeManually(jwt, request);

        String tenantId = TenantContextHolder.getTenantId();
        messagingTemplate.convertAndSend("/topic/" + tenantId + "/time-entries", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/start/{taskId}")
    public ResponseEntity<ActiveTimerResponse> startTimer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID taskId
    ) {
        ActiveTimerResponse response = timeEntryService.startTimer(jwt, taskId);

        String tenantId = TenantContextHolder.getTenantId();
        messagingTemplate.convertAndSend("/topic/" + tenantId + "/timers/start", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/stop")
    public ResponseEntity<TimeEntryResponse> stopTimer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "true") boolean isBillable) {
        TimeEntryResponse response = timeEntryService.stopTimer(jwt, isBillable);

        String tenantId = TenantContextHolder.getTenantId();
        messagingTemplate.convertAndSend("/topic/" + tenantId + "/timers/stop", response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<ActiveTimerResponse> getActiveTimer(@AuthenticationPrincipal Jwt jwt) {
        return timeEntryService.getActiveTimer(jwt)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TimeEntryResponse>> getTimeEntriesByTaskId(@PathVariable UUID taskId) {
        List<TimeEntryResponse> responses = timeEntryService.getTimeEntriesByTaskId(taskId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TimeEntryResponse>> getTimeEntriesByUserId(@PathVariable String userId) {
        List<TimeEntryResponse> responses = timeEntryService.getTimeEntriesByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeEntry(@PathVariable UUID id) {
        timeEntryService.deleteTimeEntry(id);
        return ResponseEntity.noContent().build();
    }
}
