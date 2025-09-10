package io.paymentcollection.payment.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!base-integration-test")
public class PaymentIndexInitializer {

  private final ElasticsearchClient client;

  @PostConstruct
  public void createIndexIfNotExists() throws Exception {
    String indexName = "payments";

    boolean exists = client.indices().exists(ExistsRequest.of(r -> r.index(indexName))).value();

    if (!exists) {
      client
          .indices()
          .create(
              c ->
                  c.index(indexName)
                      .settings(
                          s ->
                              s.numberOfShards("1")
                                  .numberOfReplicas("1")
                                  .refreshInterval(Time.of(t -> t.time("1s")))));
    }
  }
}
