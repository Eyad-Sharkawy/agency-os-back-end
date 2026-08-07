package dev.eyadsharkawy.agency_os_api.tenant.project.repository;

import dev.eyadsharkawy.agency_os_api.tenant.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByClientId(UUID clientId);
}
