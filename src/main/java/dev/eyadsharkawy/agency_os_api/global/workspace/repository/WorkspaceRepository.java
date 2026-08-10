package dev.eyadsharkawy.agency_os_api.global.workspace.repository;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    Optional<Workspace> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);

    void deleteByTenantId(String tenantId);

    @Query("""
                SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
                FROM Workspace w
                JOIN w.userWorkspaces uw
                WHERE uw.user.keycloakId = :keycloakId
                  AND w.tenantId = :tenantId
                  AND w.isActive = true
            """)
    boolean isUserMemberOfTenant(@Param("keycloakId") String keycloakId, @Param("tenantId") String tenantId);
}
