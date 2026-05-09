package com.example.order.dto;

import java.util.Map;

public class HealthResponse {
  public String status = "UP";
  public Map<String, String> patterns = Map.of(
    "saga", "choreography + compensation",
    "cqrs", "command tables + asynchronous read model projection"
  );
}
