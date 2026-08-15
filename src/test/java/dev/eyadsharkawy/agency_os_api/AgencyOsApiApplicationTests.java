package dev.eyadsharkawy.agency_os_api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class AgencyOsApiApplicationTests extends BaseIntegrationTest {

  @Autowired private ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    // Verifies that the Spring application context starts up and initializes all beans without
    // error
    assertThat(applicationContext).isNotNull();
  }
}
