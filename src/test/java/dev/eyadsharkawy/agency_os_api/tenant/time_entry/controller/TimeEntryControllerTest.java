package dev.eyadsharkawy.agency_os_api.tenant.time_entry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.ActiveTimerResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryRequest;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.dto.TimeEntryResponse;
import dev.eyadsharkawy.agency_os_api.tenant.time_entry.service.TimeEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeEntryController.class)
@Import(JacksonConfig.class)
public class TimeEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeEntryService timeEntryService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean(name = "workspaceSecurity")
    private WorkspaceSecurity workspaceSecurity;

    @MockitoBean
    private TenantSecurityFilter tenantSecurityFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(tenantSecurityFilter).doFilter(any(), any(), any());
    }

    @Test
    void testLogTimeManually_Success() throws Exception {
        TimeEntryRequest request = new TimeEntryRequest(UUID.randomUUID(), 60, true);
        TimeEntryResponse response = new TimeEntryResponse(UUID.randomUUID(), UUID.randomUUID(), "userId", 60, true, Instant.now(), Instant.now().plusSeconds(3600));

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(timeEntryService.logTimeManually(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/time-entries")
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testStartTimer_Success() throws Exception {
        UUID taskId = UUID.randomUUID();
        ActiveTimerResponse response = new ActiveTimerResponse("userId", taskId, Instant.now());

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(timeEntryService.startTimer(any(), eq(taskId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/time-entries/start/{taskId}", taskId)
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isCreated());
    }

    @Test
    void testStopTimer_Success() throws Exception {
        TimeEntryResponse response = new TimeEntryResponse(UUID.randomUUID(), UUID.randomUUID(), "userId", 60, true, Instant.now(), Instant.now().plusSeconds(3600));

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(timeEntryService.stopTimer(any(), eq(true))).thenReturn(response);

        mockMvc.perform(post("/api/v1/time-entries/stop")
                        .header("X-Tenant-ID", "tenant1")
                        .param("isBillable", "true")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetActiveTimer_Success() throws Exception {
        ActiveTimerResponse response = new ActiveTimerResponse("userId", UUID.randomUUID(), Instant.now());

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(timeEntryService.getActiveTimer(any())).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/time-entries/active")
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTimeEntriesByTaskId_Success() throws Exception {
        UUID taskId = UUID.randomUUID();

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(timeEntryService.getTimeEntriesByTaskId(taskId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/time-entries/task/{taskId}", taskId)
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTimeEntriesByUserId_Success() throws Exception {
        String userId = "userId";

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(timeEntryService.getTimeEntriesByUserId(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/time-entries/user/{userId}", userId)
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteTimeEntry_Success() throws Exception {
        UUID entryId = UUID.randomUUID();

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);

        mockMvc.perform(delete("/api/v1/time-entries/{id}", entryId)
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNoContent());
    }
}
