package io.paymentcollection.payment.infrastructure.persistence;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.paymentcollection.payment.domain.PaymentSearchRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 13.
 */
@Component
public class PaymentSearchQueryBuilder {

  public Query buildQuery(PaymentSearchRequest request) {
    List<Query> mustQueries = new ArrayList<>();

    if (request.getFromDate() != null && request.getToDate() != null) {

      mustQueries.add(
          Query.of(
              q ->
                  q.range(
                      r ->
                          r.date(
                              d -> {
                                var b = new DateRangeQuery.Builder().field("date");
                                if (request.getFromDate() != null)
                                  b.gte(request.getFromDate().toString());
                                if (request.getToDate() != null)
                                  b.lte(request.getToDate().toString());
                                return b; // <-- return the BUILDER, not b.build()
                              }))));
    }

    if (request.getStatus() != null && !request.getStatus().isEmpty()) {
      mustQueries.add(
          Query.of(
              q ->
                  q.terms(
                      t ->
                          t.field("status")
                              .terms(
                                  terms ->
                                      terms.value(
                                          request.getStatus().stream()
                                              .map(FieldValue::of)
                                              .toList())))));
    }

    if (request.getMinAmount() != null || request.getMaxAmount() != null) {
      mustQueries.add(
          new Query.Builder()
              .range(
                  r ->
                      r.number(
                          n -> {
                            NumberRangeQuery.Builder b =
                                new NumberRangeQuery.Builder().field("amount");
                            if (request.getMinAmount() != null) b.gte(request.getMinAmount());
                            if (request.getMaxAmount() != null) b.lte(request.getMaxAmount());
                            return b; // return the builder, not build()
                          }))
              .build());
    }

    if (request.getCustomerId() != null) {
      mustQueries.add(
          Query.of(q -> q.term(t -> t.field("customerId").value(request.getCustomerId()))));
    }

    return Query.of(q -> q.bool(b -> b.must(mustQueries)));
  }
}
