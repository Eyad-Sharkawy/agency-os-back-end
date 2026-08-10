package dev.eyadsharkawy.agency_os_api.tenant.client.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client_users")
@Getter
@Setter
@NoArgsConstructor
public class ClientUser {
  @Id
  @Column(name = "user_id", nullable = false)
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;
}
