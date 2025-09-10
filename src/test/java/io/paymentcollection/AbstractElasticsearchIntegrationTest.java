package io.paymentcollection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(SpringExtension.class)
@ActiveProfiles("elastic-test")
public abstract class AbstractElasticsearchIntegrationTest {

  @BeforeEach
  public final void init() {
    System.out.println("Elasticsearch Testcontainer URI: " + elasticsearch.getHttpHostAddress());
  }

  // Postgres
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("paymentdb")
          .withUsername("test")
          .withPassword("test")
          .withReuse(true);

  // Elasticsearch
  @Container @ServiceConnection
  static final ElasticsearchContainer elasticsearch =
      new ElasticsearchContainer(
              DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
          .withEnv("discovery.type", "single-node")
          .withEnv("xpack.security.enabled", "false")
          .withReuse(false);
}
