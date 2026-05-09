package com.example.order.dto;

import java.math.BigDecimal;

public class CreateOrderRequest {
  public String userId;
  public String sku;
  public int quantity;
  public BigDecimal amount;
  public String paymentMethod;
  public String shippingAddress;
}
