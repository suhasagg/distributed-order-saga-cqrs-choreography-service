package com.example.order.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_events")
public class SagaEventEntity {
  @Id public String id = UUID.randomUUID().toString();
  public String tenantId;
  public String orderId;
  public String sagaStep;
  public String eventType;
  @Column(length = 4000) public String details;
  public Instant createdAt = Instant.now();
}
