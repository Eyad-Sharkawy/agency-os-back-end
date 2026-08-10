package dev.eyadsharkawy.agency_os_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.flyway.ignore-migration-patterns=*:*")
class AgencyOsApiApplicationTests {

  @Test
  void contextLoads() {}
}
