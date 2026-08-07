package dev.eyadsharkawy.agency_os_api.tenant.client.contoller;

import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientRequest;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientResponse;
import dev.eyadsharkawy.agency_os_api.tenant.client.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @PostMapping
    ResponseEntity<ClientResponse> createClientBy(@Valid @RequestBody ClientRequest request) {
        ClientResponse response = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    ResponseEntity<List<ClientResponse>> getAllClients() {
        List<ClientResponse> responses = clientService.getAllClients();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    ResponseEntity<ClientResponse> getClientById(@PathVariable UUID id) {
        ClientResponse response = clientService.getClientById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    ResponseEntity<ClientResponse> updateClientById(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        ClientResponse response = clientService.updateClientById(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteClientById(@PathVariable UUID id) {
        clientService.deleteClientById(id);
        return ResponseEntity.noContent().build();
    }
}
