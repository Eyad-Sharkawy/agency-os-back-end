package dev.eyadsharkawy.agency_os_api.core.multitenancy;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateMultiTenancyConfig {

  @Bean
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
      TenantConnectionProvider tenantConnectionProvider, TenantResolver tenantResolver) {
    return hibernateProperties -> {
      hibernateProperties.put(
          AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, tenantConnectionProvider);
      hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
    };
  }
}
