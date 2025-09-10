package io.paymentcollection.payment.infrastructure.persistence;

import io.paymentcollection.payment.infrastructure.persistence.document.PaymentDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 10.
 */
@Profile("!base-integration-test")
@Repository
public interface ElasticsearchPaymentRepository
    extends ElasticsearchRepository<PaymentDocument, String> {}
