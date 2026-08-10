package dev.eyadsharkawy.agency_os_api.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                .info(new Info()
                        .title("Agency OS API Documentation")
                        .version("1.0.0")
                        .description("API documentation for the Agency OS backend workspace, project, client, invoicing, and task management services."))
                // Define the workflow tags in the exact logical order of setup/usage
                .tags(List.of(
                        new Tag().name("01. Workspaces").description("Endpoints for managing workspace organizations, membership listings, roles, and ownership transfers"),
                        new Tag().name("02. Workspace Invitations").description("Endpoints for sending, listing, accepting, and declining invitations to join workspaces"),
                        new Tag().name("03. Clients").description("Endpoints for managing client companies and customer accounts within the tenant space"),
                        new Tag().name("04. Projects").description("Endpoints for managing projects, budgets, billable rates, and task-based teammate security mappings"),
                        new Tag().name("05. Tasks").description("Endpoints for task backlog planning, assignment tracking, and status/progress updates"),
                        new Tag().name("06. Time Tracking").description("Endpoints for manual time logging, start/stop stopwatch timers, and WebSocket broadcast integrations"),
                        new Tag().name("07. Invoices").description("Endpoints for auto-generating invoices, updating billing status, listing invoices, and downloading multi-page PDFs")
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList(oauthSchemeName)
                        .addList(tenantHeaderName))
                .components(new Components()
                        .addSecuritySchemes(oauthSchemeName,
                                new SecurityScheme()
                                        .name(oauthSchemeName)
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .description("Keycloak OpenID Connect authentication server redirect")
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(authUrl)
                                                        .tokenUrl(tokenUrl)
                                                        .scopes(new Scopes()
                                                                .addString("openid", "OpenID authentication token")
                                                                .addString("profile", "User identity info profile")
                                                                .addString("email", "User registered email context")))))
                        .addSecuritySchemes(tenantHeaderName,
                                new SecurityScheme()
                                        .name("X-Tenant-ID")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Enter your active workspace tenant ID UUID")));
    }

    /**
     * Programmatically sorts all API paths and DTO Schemas logically:
     * - Paths: sorted hierarchically (/api/v1/workspaces -> /api/v1/workspaces/{tenantId})
     * - Schemas: sorted alphabetically by key so they group numerically (01. Workspaces -> 07. Invoices)
     */
    @Bean
    public OpenApiCustomizer sortPathsAndSchemasCustomizer() {
        return openApi -> {
            // Sort Paths
            Paths paths = openApi.getPaths();
            if (paths != null) {
                Paths sortedPaths = new Paths();
                paths.entrySet().stream()
                    .sorted((entry1, entry2) -> {
                        String p1 = entry1.getKey();
                        String p2 = entry2.getKey();

                        String[] s1 = p1.split("/");
                        String[] s2 = p2.split("/");

                        int minLen = Math.min(s1.length, s2.length);
                        for (int i = 0; i < minLen; i++) {
                            boolean isParam1 = s1[i].startsWith("{");
                            boolean isParam2 = s2[i].startsWith("{");

                            if (isParam1 != isParam2) {
                                return isParam1 ? 1 : -1;
                            }

                            int comp = s1[i].compareTo(s2[i]);
                            if (comp != 0) {
                                return comp;
                            }
                        }
                        return Integer.compare(s1.length, s2.length);
                    })
                    .forEach(entry -> sortedPaths.put(entry.getKey(), entry.getValue()));
                openApi.setPaths(sortedPaths);
            }

            // Sort Schemas (DTO Models)
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                Map<String, io.swagger.v3.oas.models.media.Schema> schemas = openApi.getComponents().getSchemas();
                Map<String, io.swagger.v3.oas.models.media.Schema> sortedSchemas = new LinkedHashMap<>();

                schemas.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sortedSchemas.put(entry.getKey(), entry.getValue()));

                openApi.getComponents().setSchemas(sortedSchemas);
            }
        };
    }
}
