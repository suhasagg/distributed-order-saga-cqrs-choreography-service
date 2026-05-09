package com.example.order.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {
  @Id public String id = UUID.randomUUID().toString();
  public String tenantId;
  public String orderId;
  public String sku;
  public int quantity;
  public String status;
  public Instant createdAt = Instant.now();
  public Instant updatedAt = createdAt;
}
