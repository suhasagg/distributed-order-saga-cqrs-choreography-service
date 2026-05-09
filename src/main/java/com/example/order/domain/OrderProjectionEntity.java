package com.example.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_read_model")
public class OrderProjectionEntity {
  @Id public String orderId;
  public String tenantId;
  public String userId;
  public String sku;
  public int quantity;
  public BigDecimal amount;
  public String orderStatus;
  public String paymentStatus;
  public String inventoryStatus;
  public String shipmentStatus;
  public String failureReason;
  public Instant updatedAt;
}
