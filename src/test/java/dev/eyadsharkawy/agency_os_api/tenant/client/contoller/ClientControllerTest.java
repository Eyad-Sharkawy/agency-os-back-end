package dev.eyadsharkawy.agency_os_api.tenant.client.contoller; // intentionally matching misspelled package if any

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.core.config.JacksonConfig;
import dev.eyadsharkawy.agency_os_api.core.multitenancy.TenantSecurityFilter;
import dev.eyadsharkawy.agency_os_api.core.security.WorkspaceSecurity;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientRequest;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientResponse;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientStatus;
import dev.eyadsharkawy.agency_os_api.tenant.client.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import(JacksonConfig.class)
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

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
    void testCreateClient_Success() throws Exception {
        ClientRequest request = new ClientRequest("Client Name", "test@client.com", ClientStatus.ACTIVE);
        ClientResponse response = new ClientResponse(UUID.randomUUID(), "Client Name", "test@client.com", ClientStatus.ACTIVE, Instant.now(), Instant.now());

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(clientService.createClient(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients")
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testGetAllClients_Success() throws Exception {
        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(clientService.getAllClients()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/clients")
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetClientById_Success() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(clientService.getClientById(clientId)).thenReturn(new ClientResponse(clientId, "Client", "client@test.com", ClientStatus.ACTIVE, Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/clients/{id}", clientId)
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateClientById_Success() throws Exception {
        UUID clientId = UUID.randomUUID();
        ClientRequest request = new ClientRequest("Updated Client Name", "updated@client.com", ClientStatus.INACTIVE);

        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);
        when(clientService.updateClientById(any(), any())).thenReturn(new ClientResponse(clientId, "Updated Client Name", "updated@client.com", ClientStatus.INACTIVE, Instant.now(), Instant.now()));

        mockMvc.perform(put("/api/v1/clients/{id}", clientId)
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteClientById_Success() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(workspaceSecurity.hasRole(any(String[].class))).thenReturn(true);

        mockMvc.perform(delete("/api/v1/clients/{id}", clientId)
                        .header("X-Tenant-ID", "tenant1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNoContent());
    }
}
