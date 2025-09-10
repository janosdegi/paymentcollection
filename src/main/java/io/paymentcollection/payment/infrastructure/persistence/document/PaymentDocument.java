package io.paymentcollection.payment.infrastructure.persistence.document;

import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 10.
 */
@Document(indexName = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDocument {

  @Id private String id;

  @Field(type = FieldType.Date, format = DateFormat.date_time)
  private Instant date;

  @Field(type = FieldType.Double)
  private Double amount;

  @Field(type = FieldType.Keyword)
  private String status;

  @Field(type = FieldType.Keyword)
  private String customerId;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String customerName;
}
