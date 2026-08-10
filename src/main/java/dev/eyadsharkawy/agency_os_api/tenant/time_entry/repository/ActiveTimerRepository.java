package dev.eyadsharkawy.agency_os_api.tenant.time_entry.repository;

import dev.eyadsharkawy.agency_os_api.tenant.time_entry.entity.ActiveTimer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveTimerRepository extends JpaRepository<ActiveTimer, String> {}
