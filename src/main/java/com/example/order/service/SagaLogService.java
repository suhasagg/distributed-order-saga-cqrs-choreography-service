package com.example.order.service;

import com.example.order.domain.SagaEventEntity;
import com.example.order.dto.SagaEventPayload;
import com.example.order.repository.SagaEventEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SagaLogService {
  public static final String TOPIC = "order-saga-events";
  private final SagaEventEntityRepository repo;
  private final EventPublisher publisher;
  private final ObjectMapper mapper;

  public SagaLogService(SagaEventEntityRepository repo, EventPublisher publisher, ObjectMapper mapper) {
    this.repo = repo;
    this.publisher = publisher;
    this.mapper = mapper;
  }

  public void log(String tenantId, String orderId, String step, String eventType, String details) {
    SagaEventEntity e = new SagaEventEntity();
    e.tenantId = tenantId;
    e.orderId = orderId;
    e.sagaStep = step;
    e.eventType = eventType;
    e.details = details;
    repo.save(e);
  }

  public void publish(SagaEventPayload event, String details) {
    log(event.tenantId, event.orderId, event.step, event.eventType, details);
    try {
      publisher.publish(TOPIC, event.orderId, mapper.writeValueAsString(event));
    } catch (Exception e) {
      publisher.publish(TOPIC, event.orderId, "{\"tenantId\":\"" + event.tenantId + "\",\"orderId\":\"" + event.orderId + "\",\"eventType\":\"" + event.eventType + "\"}");
    }
  }

  public List<SagaEventEntity> history(String tenantId, String orderId) {
    return repo.findByTenantIdAndOrderIdOrderByCreatedAtAsc(tenantId, orderId);
  }
}
