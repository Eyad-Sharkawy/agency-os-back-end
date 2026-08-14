package dev.eyadsharkawy.agency_os_api.tenant.project.entity;

import dev.eyadsharkawy.agency_os_api.shared.entity.BaseEntity;
import dev.eyadsharkawy.agency_os_api.tenant.client.entity.Client;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Setter
@Getter
@Entity
@Table(name = "projects")
@SQLDelete(sql = "UPDATE projects SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
public class Project extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "budget")
  private BigDecimal budget;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ProjectStatus status = ProjectStatus.PLANNING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Column(name = "billing_rate", nullable = false)
  private BigDecimal billingRate = BigDecimal.valueOf(100.00);

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;
}
