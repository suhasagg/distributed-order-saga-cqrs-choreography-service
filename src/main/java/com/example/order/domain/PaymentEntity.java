package com.example.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {
  @Id public String id = UUID.randomUUID().toString();
  public String tenantId;
  public String orderId;
  public BigDecimal amount;
  public String status;
  public String paymentMethod;
  public Instant createdAt = Instant.now();
  public Instant updatedAt = createdAt;
}
