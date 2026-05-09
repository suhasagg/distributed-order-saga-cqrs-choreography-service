package com.example.order.dto;

import com.example.order.domain.OrderProjectionEntity;
import java.math.BigDecimal;
import java.time.Instant;

public class OrderResponse {
  public String orderId;
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

  public static OrderResponse from(OrderProjectionEntity p) {
    OrderResponse r = new OrderResponse();
    r.orderId = p.orderId; r.tenantId = p.tenantId; r.userId = p.userId; r.sku = p.sku; r.quantity = p.quantity;
    r.amount = p.amount; r.orderStatus = p.orderStatus; r.paymentStatus = p.paymentStatus;
    r.inventoryStatus = p.inventoryStatus; r.shipmentStatus = p.shipmentStatus; r.failureReason = p.failureReason; r.updatedAt = p.updatedAt;
    return r;
  }
}
