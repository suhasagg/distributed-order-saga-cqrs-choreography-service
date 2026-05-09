package com.example.order.service;

import com.example.order.domain.InventoryReservationEntity;
import com.example.order.domain.OrderEntity;
import com.example.order.dto.SagaEventPayload;
import com.example.order.repository.InventoryReservationEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class InventoryService {
  private final InventoryReservationEntityRepository repo;
  private final SagaLogService sagaLog;
  private final ObjectMapper mapper;

  public InventoryService(InventoryReservationEntityRepository repo, SagaLogService sagaLog, ObjectMapper mapper) {
    this.repo = repo;
    this.sagaLog = sagaLog;
    this.mapper = mapper;
  }

  @KafkaListener(topics = SagaLogService.TOPIC, groupId = "inventory-service")
  public void onSagaEvent(String json) throws Exception {
    SagaEventPayload e = mapper.readValue(json, SagaEventPayload.class);
    if ("PAYMENT_AUTHORIZED".equals(e.eventType)) {
      reserveFromEvent(e);
    } else if ("SHIPMENT_FAILED".equals(e.eventType)) {
      release(e.tenantId, e.orderId);
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "INVENTORY", "INVENTORY_RELEASED");
      out.reason = "Compensation triggered by shipping failure";
      sagaLog.publish(out, out.reason);
    }
  }

  public InventoryReservationEntity reserve(OrderEntity order) {
    InventoryReservationEntity r = new InventoryReservationEntity();
    r.tenantId = order.tenantId;
    r.orderId = order.id;
    r.sku = order.sku;
    r.quantity = order.quantity;
    r.status = "OUT_OF_STOCK".equalsIgnoreCase(order.sku) || order.quantity > 100 ? "FAILED" : "RESERVED";
    r.updatedAt = Instant.now();
    return repo.save(r);
  }

  private void reserveFromEvent(SagaEventPayload e) {
    InventoryReservationEntity r = new InventoryReservationEntity();
    r.tenantId = e.tenantId;
    r.orderId = e.orderId;
    r.sku = e.sku;
    r.quantity = e.quantity;
    r.status = "OUT_OF_STOCK".equalsIgnoreCase(e.sku) || e.quantity > 100 ? "FAILED" : "RESERVED";
    r.updatedAt = Instant.now();
    repo.save(r);

    if ("RESERVED".equals(r.status)) {
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "INVENTORY", "INVENTORY_RESERVED");
      out.sku = e.sku;
      out.quantity = e.quantity;
      out.amount = e.amount;
      out.shippingAddress = e.shippingAddress;
      sagaLog.publish(out, "Inventory reserved by Inventory service");
    } else {
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "INVENTORY", "INVENTORY_FAILED");
      out.reason = "Inventory reservation failed";
      sagaLog.publish(out, out.reason);
    }
  }

  public void release(String tenantId, String orderId) {
    repo.findByTenantId(tenantId).stream()
      .filter(r -> r.orderId.equals(orderId))
      .filter(r -> !"RELEASED".equals(r.status))
      .findFirst()
      .ifPresent(r -> {
        r.status = "RELEASED";
        r.updatedAt = Instant.now();
        repo.save(r);
      });
  }
}
