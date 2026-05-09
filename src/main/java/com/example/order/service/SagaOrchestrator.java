package com.example.order.service;

import com.example.order.domain.*;
import com.example.order.repository.OrderEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class SagaOrchestrator {
  private final PaymentService payment;
  private final InventoryService inventory;
  private final ShippingService shipping;
  private final ReadModelService readModel;
  private final SagaLogService log;
  private final OrderEntityRepository orders;

  public SagaOrchestrator(PaymentService payment, InventoryService inventory, ShippingService shipping, ReadModelService readModel, SagaLogService log, OrderEntityRepository orders) {
    this.payment = payment; this.inventory = inventory; this.shipping = shipping; this.readModel = readModel; this.log = log; this.orders = orders;
  }

  @Transactional
  public void start(OrderEntity order, String paymentMethod, String address) {
    log.log(order.tenantId, order.id, "ORDER", "ORDER_CREATED", "Order accepted by command side");
    PaymentEntity p = payment.authorize(order, paymentMethod);
    if (!"AUTHORIZED".equals(p.status)) { fail(order, "PAYMENT_FAILED", "Payment authorization failed", "FAILED", "PENDING", "PENDING"); return; }
    log.log(order.tenantId, order.id, "PAYMENT", "PAYMENT_AUTHORIZED", "Payment authorized");
    readModel.update(order.tenantId, order.id, "PAYMENT_AUTHORIZED", "AUTHORIZED", null, null, null);

    InventoryReservationEntity r = inventory.reserve(order);
    if (!"RESERVED".equals(r.status)) {
      payment.refund(order.tenantId, order.id);
      log.log(order.tenantId, order.id, "PAYMENT", "PAYMENT_REFUNDED", "Compensation after inventory failure");
      fail(order, "INVENTORY_FAILED", "Inventory reservation failed; payment refunded", "REFUNDED", "FAILED", "PENDING"); return;
    }
    log.log(order.tenantId, order.id, "INVENTORY", "INVENTORY_RESERVED", "Inventory reserved");
    readModel.update(order.tenantId, order.id, "INVENTORY_RESERVED", "AUTHORIZED", "RESERVED", null, null);

    ShipmentEntity s = shipping.createShipment(order, address);
    if (!"CREATED".equals(s.status)) {
      inventory.release(order.tenantId, order.id);
      payment.refund(order.tenantId, order.id);
      log.log(order.tenantId, order.id, "INVENTORY", "INVENTORY_RELEASED", "Compensation after shipping failure");
      log.log(order.tenantId, order.id, "PAYMENT", "PAYMENT_REFUNDED", "Compensation after shipping failure");
      fail(order, "SHIPPING_FAILED", "Shipping failed; inventory released and payment refunded", "REFUNDED", "RELEASED", "FAILED"); return;
    }
    log.log(order.tenantId, order.id, "SHIPPING", "SHIPMENT_CREATED", "Shipment created");
    order.status = "COMPLETED"; order.updatedAt = Instant.now(); orders.save(order);
    readModel.update(order.tenantId, order.id, "COMPLETED", "AUTHORIZED", "RESERVED", "CREATED", null);
    log.log(order.tenantId, order.id, "ORDER", "ORDER_COMPLETED", "Saga completed successfully");
  }

  private void fail(OrderEntity order, String status, String reason, String paymentStatus, String inventoryStatus, String shipmentStatus) {
    order.status = status; order.failureReason = reason; order.updatedAt = Instant.now(); orders.save(order);
    readModel.update(order.tenantId, order.id, status, paymentStatus, inventoryStatus, shipmentStatus, reason);
    log.log(order.tenantId, order.id, "ORDER", status, reason);
  }
}
