package io.paymentcollection.payment.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.paymentcollection.payment.domain.PaymentSearchRequest;
import io.paymentcollection.payment.domain.PaymentSearchResponse;
import io.paymentcollection.payment.domain.PaymentSearchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 13.
 */

@Component
@RequiredArgsConstructor
public class SearchPaymentsHandler {

    private final ElasticsearchClient elasticsearchClient;
    private final io.paymentcollection.payment.infrastructure.persistence.PaymentSearchQueryBuilder queryBuilder;

    public PaymentSearchResponse handle(PaymentSearchRequest request) {
        try {
            Query query = queryBuilder.buildQuery(request);

            int from = request.getPage() * request.getSize();

            SearchResponse<PaymentSearchResultDto> response = elasticsearchClient.search(s -> s
                            .index("payments") // 👉 index name (infrastructure concern, could be config)
                            .query(query)
                            .from(from)
                            .size(request.getSize())
                            .sort(so -> so.field(f -> f
                                    .field(Optional.ofNullable(request.getSortBy()).orElse("date"))
                                    .order("DESC".equalsIgnoreCase(request.getSortOrder()) ? SortOrder.Desc : SortOrder.Asc)
                            )),
                    PaymentSearchResultDto.class
            );

            List<PaymentSearchResultDto> payments = response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .toList();

            long totalElements = response.hits().total() != null ? response.hits().total().value() : 0L;
            int totalPages = (int) Math.ceil((double) totalElements / request.getSize());

            return PaymentSearchResponse.builder()
                    .payments(payments)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .page(request.getPage())
                    .size(request.getSize())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to search payments in Elasticsearch", e);
        }
    }
}
