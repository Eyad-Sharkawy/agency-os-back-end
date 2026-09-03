package dev.eyadsharkawy.agency_os_api.global.workspace.repository;

import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspace;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.UserWorkspaceId;
import dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace, UserWorkspaceId> {

  @Query(
      """
                SELECT uw.role
                FROM UserWorkspace uw
                WHERE uw.user.keycloakId = :keycloakId
                  AND uw.workspace.tenantId = :tenantId
            """)
  Optional<WorkspaceRole> findRoleByKeycloakIdAndTenantId(
      @Param("keycloakId") String keycloakId, @Param("tenantId") String tenantId);

  @Query(
      """
        SELECT COUNT(uw) > 0
        FROM UserWorkspace uw
        WHERE uw.user.keycloakId IN :keycloakIds
          AND uw.workspace.tenantId = :tenantId
          AND uw.role = dev.eyadsharkawy.agency_os_api.global.workspace.entity.WorkspaceRole.CLIENT
      """)
  boolean hasClientRoleAssignee(
      @Param("keycloakIds") java.util.Collection<String> keycloakIds,
      @Param("tenantId") String tenantId);
}
