package com.example.order.controller;

import com.example.order.dto.*;
import com.example.order.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class OrderController {
  private final OrderCommandService commands;
  private final OrderQueryService queries;
  public OrderController(OrderCommandService commands, OrderQueryService queries) { this.commands = commands; this.queries = queries; }

  @PostMapping("/orders")
  public OrderResponse create(@RequestHeader("X-Tenant-ID") String tenantId, @RequestBody CreateOrderRequest req) {
    var order = commands.create(tenantId, req);
    return queries.get(tenantId, order.id);
  }

  @GetMapping("/orders/{orderId}")
  public OrderResponse get(@RequestHeader("X-Tenant-ID") String tenantId, @PathVariable("orderId") String orderId) {
    return queries.get(tenantId, orderId);
  }

  @GetMapping("/orders")
  public List<OrderResponse> list(@RequestHeader("X-Tenant-ID") String tenantId) { return queries.list(tenantId); }

  @GetMapping("/orders/{orderId}/saga-events")
  public List<SagaEventResponse> saga(@RequestHeader("X-Tenant-ID") String tenantId, @PathVariable("orderId") String orderId) {
    return queries.events(tenantId, orderId);
  }

  @GetMapping("/health")
  public HealthResponse health() { return new HealthResponse(); }
}
