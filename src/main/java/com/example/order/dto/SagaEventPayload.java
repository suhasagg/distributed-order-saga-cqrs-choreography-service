package com.example.order.dto;

import java.math.BigDecimal;

public class SagaEventPayload {
  public String tenantId;
  public String orderId;
  public String step;
  public String eventType;
  public String userId;
  public String sku;
  public int quantity;
  public BigDecimal amount;
  public String paymentMethod;
  public String shippingAddress;
  public String reason;

  public static SagaEventPayload of(String tenantId, String orderId, String step, String eventType) {
    SagaEventPayload e = new SagaEventPayload();
    e.tenantId = tenantId;
    e.orderId = orderId;
    e.step = step;
    e.eventType = eventType;
    return e;
  }
}
