package dev.eyadsharkawy.agency_os_api.tenant.project.repository;

import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  List<Project> findByClientId(UUID clientId);

  @Query(
      """
                SELECT DISTINCT p
                FROM Project p
                JOIN Task t ON t.project.id = p.id
                JOIN t.assigneeIds a
                WHERE a = :keycloakId
            """)
  List<Project> findProjectsByAssigneeKeycloakId(
      @org.springframework.data.repository.query.Param("keycloakId") String keycloakId);

  @Query(
      """
                SELECT COUNT(t) > 0
                FROM Task t
                JOIN t.assigneeIds a
                WHERE t.project.id = :projectId
                  AND a = :keycloakId
            """)
  boolean isUserAssignedToProject(
      @org.springframework.data.repository.query.Param("projectId") UUID projectId,
      @org.springframework.data.repository.query.Param("keycloakId") String keycloakId);
}
