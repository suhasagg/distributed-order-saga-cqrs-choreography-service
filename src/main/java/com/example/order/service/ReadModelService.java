package com.example.order.service;

import com.example.order.domain.OrderEntity;
import com.example.order.domain.OrderProjectionEntity;
import com.example.order.dto.SagaEventPayload;
import com.example.order.repository.OrderEntityRepository;
import com.example.order.repository.OrderProjectionEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ReadModelService {
  private final OrderProjectionEntityRepository repo;
  private final OrderEntityRepository orders;
  private final ObjectMapper mapper;

  public ReadModelService(OrderProjectionEntityRepository repo, OrderEntityRepository orders, ObjectMapper mapper) {
    this.repo = repo;
    this.orders = orders;
    this.mapper = mapper;
  }

  @KafkaListener(topics = SagaLogService.TOPIC, groupId = "order-read-model-projector")
  public void onSagaEvent(String json) throws Exception {
    SagaEventPayload e = mapper.readValue(json, SagaEventPayload.class);
    switch (e.eventType) {
      case "ORDER_CREATED" -> initFromEvent(e);
      case "PAYMENT_AUTHORIZED" -> update(e.tenantId, e.orderId, "PAYMENT_AUTHORIZED", "AUTHORIZED", null, null, null);
      case "PAYMENT_FAILED" -> markOrderFailed(e.tenantId, e.orderId, "PAYMENT_FAILED", "Payment failed", "FAILED", "PENDING", "PENDING");
      case "INVENTORY_RESERVED" -> update(e.tenantId, e.orderId, "INVENTORY_RESERVED", "AUTHORIZED", "RESERVED", null, null);
      case "INVENTORY_FAILED" -> markOrderFailed(e.tenantId, e.orderId, "INVENTORY_FAILED", "Inventory failed; payment refund requested", "AUTHORIZED", "FAILED", "PENDING");
      case "SHIPMENT_CREATED" -> complete(e.tenantId, e.orderId);
      case "SHIPMENT_FAILED" -> markOrderFailed(e.tenantId, e.orderId, "SHIPPING_FAILED", "Shipping failed; compensation requested", "AUTHORIZED", "RESERVED", "FAILED");
      case "PAYMENT_REFUNDED" -> update(e.tenantId, e.orderId, null, "REFUNDED", null, null, null);
      case "INVENTORY_RELEASED" -> update(e.tenantId, e.orderId, null, null, "RELEASED", null, null);
      default -> { }
    }
  }

  private void initFromEvent(SagaEventPayload e) {
    OrderProjectionEntity p = repo.findByTenantIdAndOrderId(e.tenantId, e.orderId).orElseGet(OrderProjectionEntity::new);
    p.orderId = e.orderId;
    p.tenantId = e.tenantId;
    if (e.userId != null) p.userId = e.userId;
    if (e.sku != null) p.sku = e.sku;
    if (e.quantity > 0) p.quantity = e.quantity;
    if (e.amount != null) p.amount = e.amount;
    p.orderStatus = "ORDER_CREATED";
    if (p.paymentStatus == null) p.paymentStatus = "PENDING";
    if (p.inventoryStatus == null) p.inventoryStatus = "PENDING";
    if (p.shipmentStatus == null) p.shipmentStatus = "PENDING";
    p.updatedAt = Instant.now();
    repo.save(p);
  }

  public void init(OrderEntity o) {
    OrderProjectionEntity p = new OrderProjectionEntity();
    p.orderId = o.id;
    p.tenantId = o.tenantId;
    p.userId = o.userId;
    p.sku = o.sku;
    p.quantity = o.quantity;
    p.amount = o.amount;
    p.orderStatus = o.status;
    p.paymentStatus = "PENDING";
    p.inventoryStatus = "PENDING";
    p.shipmentStatus = "PENDING";
    p.updatedAt = Instant.now();
    repo.save(p);
  }

  public void update(String tenantId, String orderId, String orderStatus, String paymentStatus, String inventoryStatus, String shipmentStatus, String reason) {
    OrderProjectionEntity p = repo.findByTenantIdAndOrderId(tenantId, orderId).orElseGet(() -> {
      OrderProjectionEntity created = new OrderProjectionEntity();
      created.orderId = orderId;
      created.tenantId = tenantId;
      created.paymentStatus = "PENDING";
      created.inventoryStatus = "PENDING";
      created.shipmentStatus = "PENDING";
      return created;
    });
    if (orderStatus != null) p.orderStatus = orderStatus;
    if (paymentStatus != null) p.paymentStatus = paymentStatus;
    if (inventoryStatus != null) p.inventoryStatus = inventoryStatus;
    if (shipmentStatus != null) p.shipmentStatus = shipmentStatus;
    if (reason != null) p.failureReason = reason;
    p.updatedAt = Instant.now();
    repo.save(p);
  }

  private void complete(String tenantId, String orderId) {
    update(tenantId, orderId, "COMPLETED", "AUTHORIZED", "RESERVED", "CREATED", null);
    orders.findByTenantIdAndId(tenantId, orderId).ifPresent(o -> {
      o.status = "COMPLETED";
      o.failureReason = null;
      o.updatedAt = Instant.now();
      orders.save(o);
    });
  }

  private void markOrderFailed(String tenantId, String orderId, String status, String reason, String paymentStatus, String inventoryStatus, String shipmentStatus) {
    update(tenantId, orderId, status, paymentStatus, inventoryStatus, shipmentStatus, reason);
    orders.findByTenantIdAndId(tenantId, orderId).ifPresent(o -> {
      o.status = status;
      o.failureReason = reason;
      o.updatedAt = Instant.now();
      orders.save(o);
    });
  }

  public Optional<OrderProjectionEntity> get(String tenantId, String orderId) {
    return repo.findByTenantIdAndOrderId(tenantId, orderId);
  }

  public List<OrderProjectionEntity> list(String tenantId) {
    return repo.findTop50ByTenantIdOrderByUpdatedAtDesc(tenantId);
  }
}
