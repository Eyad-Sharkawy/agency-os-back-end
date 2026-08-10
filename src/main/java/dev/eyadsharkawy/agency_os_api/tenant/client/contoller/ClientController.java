package dev.eyadsharkawy.agency_os_api.tenant.client.contoller;

import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientRequest;
import dev.eyadsharkawy.agency_os_api.tenant.client.dto.ClientResponse;
import dev.eyadsharkawy.agency_os_api.tenant.client.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(
    name = "03. Clients",
    description =
        "Endpoints for managing client companies and customer accounts within the tenant space")
public class ClientController {

  private final ClientService clientService;

  @PostMapping
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN')")
  @Operation(
      summary = "Create client company",
      description =
          "Registers a new client company within the current workspace tenant. Restricted to OWNER or ADMIN.")
  ResponseEntity<ClientResponse> createClientBy(@Valid @RequestBody ClientRequest request) {
    ClientResponse response = clientService.createClient(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER')")
  @Operation(
      summary = "List all clients",
      description =
          "Retrieves all registered client companies in the active tenant. Restricted to OWNER, ADMIN, or MEMBER.")
  ResponseEntity<List<ClientResponse>> getAllClients() {
    List<ClientResponse> responses = clientService.getAllClients();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{id}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER', 'ADMIN', 'MEMBER')")
  @Operation(
      summary = "Get client by ID",
      description =
          "Retrieves metadata of a specific client company by its unique identifier. Restricted to OWNER, ADMIN, or MEMBER.")
  ResponseEntity<ClientResponse> getClientById(
      @Parameter(description = "The client company unique ID") @PathVariable UUID id) {
    ClientResponse response = clientService.getClientById(id);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
  @Operation(
      summary = "Update client details",
      description =
          "Modifies registration and profile details of a client company. Restricted strictly to the OWNER.")
  ResponseEntity<ClientResponse> updateClientById(
      @Parameter(description = "The client company unique ID") @PathVariable UUID id,
      @Valid @RequestBody ClientRequest request) {
    ClientResponse response = clientService.updateClientById(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@workspaceSecurity.hasRole('OWNER')")
  @Operation(
      summary = "Delete client company",
      description =
          "Deletes a client company from the active tenant space. Restricted strictly to the OWNER.")
  ResponseEntity<Void> deleteClientById(
      @Parameter(description = "The client company unique ID") @PathVariable UUID id) {
    clientService.deleteClientById(id);
    return ResponseEntity.noContent().build();
  }
}
