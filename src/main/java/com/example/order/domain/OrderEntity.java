package com.example.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
  @Id public String id;
  public String tenantId;
  public String userId;
  public String sku;
  public int quantity;
  public BigDecimal amount;
  public String status;
  public String failureReason;
  public Instant createdAt;
  public Instant updatedAt;

  public static OrderEntity create(String tenantId, String userId, String sku, int quantity, BigDecimal amount) {
    OrderEntity o = new OrderEntity();
    o.id = UUID.randomUUID().toString();
    o.tenantId = tenantId;
    o.userId = userId;
    o.sku = sku;
    o.quantity = quantity;
    o.amount = amount;
    o.status = "PENDING";
    o.createdAt = Instant.now();
    o.updatedAt = o.createdAt;
    return o;
  }
}
