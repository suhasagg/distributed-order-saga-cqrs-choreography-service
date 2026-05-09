package com.example.order.service;

import com.example.order.domain.OrderEntity;
import com.example.order.domain.ShipmentEntity;
import com.example.order.dto.SagaEventPayload;
import com.example.order.repository.ShipmentEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class ShippingService {
  private final ShipmentEntityRepository repo;
  private final SagaLogService sagaLog;
  private final ObjectMapper mapper;

  public ShippingService(ShipmentEntityRepository repo, SagaLogService sagaLog, ObjectMapper mapper) {
    this.repo = repo;
    this.sagaLog = sagaLog;
    this.mapper = mapper;
  }

  @KafkaListener(topics = SagaLogService.TOPIC, groupId = "shipping-service")
  public void onSagaEvent(String json) throws Exception {
    SagaEventPayload e = mapper.readValue(json, SagaEventPayload.class);
    if ("INVENTORY_RESERVED".equals(e.eventType)) {
      createShipmentFromEvent(e);
    }
  }

  public ShipmentEntity createShipment(OrderEntity order, String address) {
    ShipmentEntity s = new ShipmentEntity();
    s.tenantId = order.tenantId;
    s.orderId = order.id;
    s.address = address;
    s.status = address != null && address.toUpperCase().contains("FAIL_SHIPPING") ? "FAILED" : "CREATED";
    s.updatedAt = Instant.now();
    return repo.save(s);
  }

  private void createShipmentFromEvent(SagaEventPayload e) {
    ShipmentEntity s = new ShipmentEntity();
    s.tenantId = e.tenantId;
    s.orderId = e.orderId;
    s.address = e.shippingAddress;
    s.status = e.shippingAddress != null && e.shippingAddress.toUpperCase().contains("FAIL_SHIPPING") ? "FAILED" : "CREATED";
    s.updatedAt = Instant.now();
    repo.save(s);

    if ("CREATED".equals(s.status)) {
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "SHIPPING", "SHIPMENT_CREATED");
      sagaLog.publish(out, "Shipment created by Shipping service");
    } else {
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "SHIPPING", "SHIPMENT_FAILED");
      out.reason = "Shipping creation failed";
      sagaLog.publish(out, out.reason);
    }
  }

  public void cancel(String tenantId, String orderId) {
    repo.findByTenantId(tenantId).stream()
      .filter(s -> s.orderId.equals(orderId))
      .findFirst()
      .ifPresent(s -> {
        s.status = "CANCELLED";
        s.updatedAt = Instant.now();
        repo.save(s);
      });
  }
}
