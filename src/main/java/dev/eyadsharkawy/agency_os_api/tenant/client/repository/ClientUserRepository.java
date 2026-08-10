package dev.eyadsharkawy.agency_os_api.tenant.client.repository;

import dev.eyadsharkawy.agency_os_api.tenant.client.entity.ClientUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientUserRepository extends JpaRepository<ClientUser, String> {
}
