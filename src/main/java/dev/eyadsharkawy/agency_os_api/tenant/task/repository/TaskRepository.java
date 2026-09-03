package dev.eyadsharkawy.agency_os_api.tenant.task.repository;

import dev.eyadsharkawy.agency_os_api.tenant.task.entity.Task;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
  List<Task> findByProjectId(UUID projectId);

  @Query("SELECT t FROM Task t JOIN t.assigneeIds a WHERE a = :userId")
  List<Task> findByAssigneeId(@Param("userId") String userId);

  @Query("SELECT t FROM Task t WHERE t.project.client.id = :clientId")
  List<Task> findByProjectClientId(@Param("clientId") UUID clientId);
}
