package dev.eyadsharkawy.agency_os_api.core.multitenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyadsharkawy.agency_os_api.global.workspace.repository.WorkspaceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSecurityFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String PUBLIC_API_PREFIX = "/api/v1/public/";

    private static final List<String> GLOBAL_ENDPOINTS = List.of(
            "/api/v1/workspaces",
            "/error",
            "/ws-timer"
    );

    private final ObjectMapper objectMapper;
    private final WorkspaceRepository workspaceRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.startsWith(PUBLIC_API_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isGlobalEndpoint = GLOBAL_ENDPOINTS.stream().anyMatch(requestURI::startsWith);
        if (isGlobalEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestedTenantId = request.getHeader(TENANT_HEADER);
        if (requestedTenantId == null || requestedTenantId.isBlank()) {
            log.warn("Rejected request to [{}]: Missing required header [{}]", requestURI, TENANT_HEADER);
            writeProblemDetail(response, HttpStatus.BAD_REQUEST, "Missing required header: " + TENANT_HEADER);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();

            boolean isAuthorized = workspaceRepository.isUserMemberOfTenant(keycloakUserId, requestedTenantId);

            if (!isAuthorized) {
                log.warn("Access denied for user [{}] attempting to access tenant [{}]", keycloakUserId, requestedTenantId);
                writeProblemDetail(response, HttpStatus.FORBIDDEN, "Access denied to workspace: " + requestedTenantId);
                return;
            }
        }

        log.debug("Tenant context established for workspace: [{}]", requestedTenantId);
        TenantContextHolder.setTenantId(requestedTenantId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void writeProblemDetail(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("timestamp", Instant.now());

        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
