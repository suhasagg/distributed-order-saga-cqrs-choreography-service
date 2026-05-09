package com.example.order.dto;

import com.example.order.domain.SagaEventEntity;
import java.time.Instant;

public class SagaEventResponse {
  public String step;
  public String eventType;
  public String details;
  public Instant createdAt;
  public static SagaEventResponse from(SagaEventEntity e) {
    SagaEventResponse r = new SagaEventResponse();
    r.step = e.sagaStep; r.eventType = e.eventType; r.details = e.details; r.createdAt = e.createdAt; return r;
  }
}
