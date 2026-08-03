package dev.eyadsharkawy.agency_os_api.core.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class TenantSecurityFilter extends OncePerRequestFilter {
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/api/public/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestedTenantId = request.getHeader(TENANT_HEADER);

        if (requestedTenantId == null || requestedTenantId.isBlank()) {
            log.warn("Rejected request to [{}]: Missing required header [{}]", request.getRequestURI(), TENANT_HEADER);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required header: " + TENANT_HEADER);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            List<String> authorizedWorkspaces = jwt.getClaimAsStringList("workspaces");

            if (authorizedWorkspaces != null && !authorizedWorkspaces.contains(requestedTenantId)) {
                log.warn("Access denied for user [{}] attempting to access unauthorized workspace [{}]",
                        jwt.getSubject(), requestedTenantId);

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Access denied to workspace: " + requestedTenantId);
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
}
