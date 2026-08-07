package dev.eyadsharkawy.agency_os_api.tenant.client.repository;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
}
