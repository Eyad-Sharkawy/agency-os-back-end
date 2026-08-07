package dev.eyadsharkawy.agency_os_api.tenant.client.entity;

import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Setter
@Getter
@Entity
@Table(name = "clients")
@SQLDelete(sql = "UPDATE clients SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public class Client extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClientStatus status = ClientStatus.PROSPECT;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
