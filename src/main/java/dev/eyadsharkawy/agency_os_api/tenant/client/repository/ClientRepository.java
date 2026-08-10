package dev.eyadsharkawy.agency_os_api.tenant.client.repository;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {}
