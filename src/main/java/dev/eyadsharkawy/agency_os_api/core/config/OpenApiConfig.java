package dev.eyadsharkawy.agency_os_api.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  @Bean
  public OpenAPI customOpenAPI() {
    final String oauthSchemeName = "keycloakAuth";
    final String tenantHeaderName = "tenantHeader";

    String authUrl = issuerUri + "/protocol/openid-connect/auth";
    String tokenUrl = issuerUri + "/protocol/openid-connect/token";

    return new OpenAPI()
        .info(
            new Info()
                .title("Agency OS API Documentation")
                .version("1.0.0")
                .description(
                    "API documentation for the Agency OS backend workspace, project, client, invoicing, and task management services."))
        // Define the workflow tags in the exact logical order of setup/usage
        .tags(
            List.of(
                new Tag()
                    .name("01. Workspaces")
                    .description(
                        "Endpoints for managing workspace organizations, membership listings, roles, and ownership transfers"),
                new Tag()
                    .name("02. Workspace Invitations")
                    .description(
                        "Endpoints for sending, listing, accepting, and declining invitations to join workspaces"),
                new Tag()
                    .name("03. Clients")
                    .description(
                        "Endpoints for managing client companies and customer accounts within the tenant space"),
                new Tag()
                    .name("04. Projects")
                    .description(
                        "Endpoints for managing projects, budgets, billable rates, and task-based teammate security mappings"),
                new Tag()
                    .name("05. Tasks")
                    .description(
                        "Endpoints for task backlog planning, assignment tracking, and status/progress updates"),
                new Tag()
                    .name("06. Time Tracking")
                    .description(
                        "Endpoints for manual time logging, start/stop stopwatch timers, and WebSocket broadcast integrations"),
                new Tag()
                    .name("07. Invoices")
                    .description(
                        "Endpoints for auto-generating invoices, updating billing status, listing invoices, and downloading multi-page PDFs")))
        .addSecurityItem(
            new SecurityRequirement().addList(oauthSchemeName).addList(tenantHeaderName))
        .components(
            new Components()
                .addSecuritySchemes(
                    oauthSchemeName,
                    new SecurityScheme()
                        .name(oauthSchemeName)
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("Keycloak OpenID Connect authentication server redirect")
                        .flows(
                            new OAuthFlows()
                                .authorizationCode(
                                    new OAuthFlow()
                                        .authorizationUrl(authUrl)
                                        .tokenUrl(tokenUrl)
                                        .scopes(
                                            new Scopes()
                                                .addString("openid", "OpenID authentication token")
                                                .addString("profile", "User identity info profile")
                                                .addString(
                                                    "email", "User registered email context")))))
                .addSecuritySchemes(
                    tenantHeaderName,
                    new SecurityScheme()
                        .name("X-Tenant-ID")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .description("Enter your active workspace tenant ID UUID")));
  }

  /**
   * Automatically attaches 401 Unauthorized and 403 Forbidden response schemas to all documented
   * operations.
   */
  @Bean
  public OpenApiCustomizer globalSecurityResponsesCustomizer() {
    return openApi -> {
      if (openApi.getPaths() != null) {
        openApi.getPaths().values().forEach(this::addSecurityResponsesToPathItem);
      }
    };
  }

  private void addSecurityResponsesToPathItem(PathItem pathItem) {
    if (pathItem != null && pathItem.readOperations() != null) {
      pathItem.readOperations().forEach(this::addSecurityResponsesToOperation);
    }
  }

  private void addSecurityResponsesToOperation(Operation operation) {
    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      return;
    }
    addResponseIfNotPresent(
        responses, "401", "Unauthorized - Full authentication is required to access this resource");
    addResponseIfNotPresent(
        responses, "403", "Forbidden - Insufficient permissions to access this resource");
  }

  private void addResponseIfNotPresent(
      ApiResponses responses, String statusCode, String description) {
    if (!responses.containsKey(statusCode)) {
      responses.addApiResponse(statusCode, new ApiResponse().description(description));
    }
  }

  /**
   * Programmatically sorts all API paths and DTO Schemas logically: - Paths: sorted hierarchically
   * (/api/v1/workspaces -> /api/v1/workspaces/{tenantId}) - Schemas: sorted alphabetically by key
   * so they group numerically (01. Workspaces -> 07. Invoices)
   */
  @Bean
  public OpenApiCustomizer sortPathsAndSchemasCustomizer() {
    return openApi -> {
      sortPaths(openApi);
      sortSchemas(openApi);
    };
  }

  private void sortPaths(OpenAPI openApi) {
    Paths paths = openApi.getPaths();
    if (paths == null) {
      return;
    }
    Paths sortedPaths = new Paths();
    paths.entrySet().stream()
        .sorted((entry1, entry2) -> comparePaths(entry1.getKey(), entry2.getKey()))
        .forEach(entry -> sortedPaths.put(entry.getKey(), entry.getValue()));
    openApi.setPaths(sortedPaths);
  }

  private int comparePaths(String p1, String p2) {
    String[] s1 = p1.split("/");
    String[] s2 = p2.split("/");

    int minLen = Math.min(s1.length, s2.length);
    for (int i = 0; i < minLen; i++) {
      int comp = comparePathSegments(s1[i], s2[i]);
      if (comp != 0) {
        return comp;
      }
    }
    return Integer.compare(s1.length, s2.length);
  }

  private int comparePathSegments(String segment1, String segment2) {
    boolean isParam1 = segment1.startsWith("{");
    boolean isParam2 = segment2.startsWith("{");

    if (isParam1 != isParam2) {
      return isParam1 ? 1 : -1;
    }
    return segment1.compareTo(segment2);
  }

  private void sortSchemas(OpenAPI openApi) {
    if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
      return;
    }
    Map<String, io.swagger.v3.oas.models.media.Schema> schemas =
        openApi.getComponents().getSchemas();
    Map<String, io.swagger.v3.oas.models.media.Schema> sortedSchemas = new LinkedHashMap<>();

    schemas.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> sortedSchemas.put(entry.getKey(), entry.getValue()));

    openApi.getComponents().setSchemas(sortedSchemas);
  }
}
