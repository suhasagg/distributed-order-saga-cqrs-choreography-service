package com.example.order.service;

import com.example.order.domain.OrderProjectionEntity;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.SagaEventResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderQueryService {
  private final ReadModelService readModel;
  private final SagaLogService sagaLog;
  public OrderQueryService(ReadModelService readModel, SagaLogService sagaLog) { this.readModel = readModel; this.sagaLog = sagaLog; }
  public OrderResponse get(String tenantId, String orderId) { return readModel.get(tenantId, orderId).map(OrderResponse::from).orElseThrow(); }
  public List<OrderResponse> list(String tenantId) { return readModel.list(tenantId).stream().map(OrderResponse::from).toList(); }
  public List<SagaEventResponse> events(String tenantId, String orderId) { return sagaLog.history(tenantId, orderId).stream().map(SagaEventResponse::from).toList(); }
}
