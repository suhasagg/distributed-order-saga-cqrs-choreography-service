package com.example.order.service;

import com.example.order.domain.OrderEntity;
import com.example.order.domain.PaymentEntity;
import com.example.order.dto.SagaEventPayload;
import com.example.order.repository.PaymentEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class PaymentService {
  private final PaymentEntityRepository repo;
  private final SagaLogService sagaLog;
  private final ObjectMapper mapper;

  public PaymentService(PaymentEntityRepository repo, SagaLogService sagaLog, ObjectMapper mapper) {
    this.repo = repo;
    this.sagaLog = sagaLog;
    this.mapper = mapper;
  }

  @KafkaListener(topics = SagaLogService.TOPIC, groupId = "payment-service")
  public void onSagaEvent(String json) throws Exception {
    SagaEventPayload e = mapper.readValue(json, SagaEventPayload.class);
    if ("ORDER_CREATED".equals(e.eventType)) {
      authorizeFromEvent(e);
    } else if ("INVENTORY_FAILED".equals(e.eventType) || "SHIPMENT_FAILED".equals(e.eventType)) {
      refund(e.tenantId, e.orderId);
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "PAYMENT", "PAYMENT_REFUNDED");
      out.reason = "Compensation triggered by " + e.eventType;
      sagaLog.publish(out, out.reason);
    }
  }

  public PaymentEntity authorize(OrderEntity order, String paymentMethod) {
    PaymentEntity p = new PaymentEntity();
    p.tenantId = order.tenantId;
    p.orderId = order.id;
    p.amount = order.amount;
    p.paymentMethod = paymentMethod;
    p.status = "FAIL_PAYMENT".equalsIgnoreCase(paymentMethod) ? "FAILED" : "AUTHORIZED";
    p.updatedAt = Instant.now();
    return repo.save(p);
  }

  private void authorizeFromEvent(SagaEventPayload e) {
    PaymentEntity p = new PaymentEntity();
    p.tenantId = e.tenantId;
    p.orderId = e.orderId;
    p.amount = e.amount;
    p.paymentMethod = e.paymentMethod;
    p.status = "FAIL_PAYMENT".equalsIgnoreCase(e.paymentMethod) ? "FAILED" : "AUTHORIZED";
    p.updatedAt = Instant.now();
    repo.save(p);

    if ("AUTHORIZED".equals(p.status)) {
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "PAYMENT", "PAYMENT_AUTHORIZED");
      out.sku = e.sku;
      out.quantity = e.quantity;
      out.amount = e.amount;
      out.paymentMethod = e.paymentMethod;
      out.shippingAddress = e.shippingAddress;
      sagaLog.publish(out, "Payment authorized by Payment service");
    } else {
      SagaEventPayload out = SagaEventPayload.of(e.tenantId, e.orderId, "PAYMENT", "PAYMENT_FAILED");
      out.reason = "Payment authorization failed";
      sagaLog.publish(out, out.reason);
    }
  }

  public void refund(String tenantId, String orderId) {
    repo.findByTenantId(tenantId).stream()
      .filter(p -> p.orderId.equals(orderId))
      .filter(p -> !"REFUNDED".equals(p.status))
      .findFirst()
      .ifPresent(p -> {
        p.status = "REFUNDED";
        p.updatedAt = Instant.now();
        repo.save(p);
      });
  }
}
