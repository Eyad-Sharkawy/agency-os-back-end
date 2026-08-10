package dev.eyadsharkawy.agency_os_api.global.workspace.repository;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspaceId;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace, UserWorkspaceId> {

    @Query("""
                SELECT uw.role
                FROM UserWorkspace uw
                WHERE uw.user.keycloakId = :keycloakId
                  AND uw.workspace.tenantId = :tenantId
            """)
    Optional<WorkspaceRole> findRoleByKeycloakIdAndTenantId(
            @Param("keycloakId") String keycloakId,
            @Param("tenantId") String tenantId
    );
}
