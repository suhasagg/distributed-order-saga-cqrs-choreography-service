package com.example.order.service;

import com.example.order.domain.OrderEntity;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.SagaEventPayload;
import com.example.order.repository.OrderEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class OrderCommandService {
  private final OrderEntityRepository orders;
  private final ReadModelService readModel;
  private final SagaLogService sagaLog;

  public OrderCommandService(OrderEntityRepository orders, ReadModelService readModel, SagaLogService sagaLog) {
    this.orders = orders;
    this.readModel = readModel;
    this.sagaLog = sagaLog;
  }

  @Transactional
  public OrderEntity create(String tenantId, CreateOrderRequest req) {
    OrderEntity o = OrderEntity.create(tenantId, req.userId, req.sku, req.quantity, req.amount);
    o.status = "ORDER_CREATED";
    o.updatedAt = Instant.now();
    orders.save(o);
    readModel.init(o);

    SagaEventPayload event = SagaEventPayload.of(tenantId, o.id, "ORDER", "ORDER_CREATED");
    event.userId = req.userId;
    event.sku = req.sku;
    event.quantity = req.quantity;
    event.amount = req.amount;
    event.paymentMethod = req.paymentMethod;
    event.shippingAddress = req.shippingAddress;
    sagaLog.publish(event, "Order command accepted; choreography saga started");
    return o;
  }
}
