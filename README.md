# Distributed Order Saga CQRS Service

A multi-tenant, event-driven order management prototype designed to demonstrate:

- Choreography-based Saga microservices pattern
- CQRS pattern

This project implements:

- Spring Boot API layer
- PostgreSQL as command-side source of truth
- PostgreSQL read model for CQRS query side
- Kafka for choreography saga events
- Order bounded context
- Payment bounded context
- Inventory bounded context
- Shipping bounded context
- Read model projector
- Saga event audit trail
- Compensation flows
- Tenant isolation using `X-Tenant-ID`

The prototype is intentionally compact and runnable as a single Spring Boot service, but the package boundaries, Kafka event handlers, and event flow mirror how the same system would be split into real microservices in production.

---

## 1. Problem Statement

This service is designed for a scenario where an e-commerce or marketplace platform needs to:

- create customer orders
- authorize payments
- reserve inventory
- create shipments
- handle failures safely
- compensate already completed steps
- expose a query-optimized order view
- keep a saga event trail for debugging
- support multi-tenant isolation
- demonstrate microservice transaction patterns

The system separates:

- command-side writes
- query-side read model
- choreography saga events
- bounded-context service logic
- event publication
- compensation actions

---

## 2. Architecture Overview

### High-level architecture

```text
                           +----------------------+
                           |       Clients        |
                           | UI / Admin / Backend |
                           +----------+-----------+
                                      |
                                      v
                         +------------+-------------+
                         |      Spring Boot API     |
                         | Commands + Queries       |
                         +------+-------------+-----+
                                |             |
                    Command side|             | Query side
                                |             |
                                v             v
                     +----------+---+    +----+----------------+
                     | Order Command|    | Order Read Model    |
                     | Tables       |    | CQRS Projection     |
                     +------+-------+    +---------+-----------+
                            |                      ^
                            v                      |
                  +---------+------------+         |
                  | Kafka Topic          |---------+
                  | order-saga-events    |  Read Model Projector
                  +----+----------+------+
                       |          |
          ORDER_CREATED|          |domain events
                       v          v
              +--------+--------+     +------------------+
              | Payment Service |---->| Inventory Service|
              | authorize/refund|     | reserve/release  |
              +--------+--------+     +---------+--------+
                       ^                        |
                       |                        v
                       |              +---------+--------+
                       +--------------| Shipping Service |
                         compensation | create shipment  |
                                      +------------------+
```

### Choreography flow

```text
ORDER_CREATED
  -> PaymentService consumes event
  -> PAYMENT_AUTHORIZED or PAYMENT_FAILED
  -> InventoryService consumes PAYMENT_AUTHORIZED
  -> INVENTORY_RESERVED or INVENTORY_FAILED
  -> ShippingService consumes INVENTORY_RESERVED
  -> SHIPMENT_CREATED or SHIPMENT_FAILED
  -> ReadModelProjector consumes all events and updates CQRS projection
```

### Compensation flow

```text
INVENTORY_FAILED
  -> PaymentService consumes event
  -> PAYMENT_REFUNDED

SHIPMENT_FAILED
  -> InventoryService consumes event
  -> INVENTORY_RELEASED
  -> PaymentService consumes event
  -> PAYMENT_REFUNDED
```

---

## 3. Core Architectural Choices

### PostgreSQL command model

Used to persist canonical order, payment, inventory reservation, shipment, and saga event records.

### CQRS read model

The query API does not assemble state from all write-side tables. It reads from `order_read_model`, a projection optimized for API reads.

### Choreography-based saga

There is no central orchestrator making all decisions. Each bounded context listens to events and publishes the next event.

### Compensation

If one step fails after earlier steps succeeded, the saga performs compensating actions using events.

Examples:

- inventory failure triggers payment refund
- shipping failure triggers inventory release and payment refund

### Kafka

Kafka carries saga events between bounded contexts.

Topic:

```text
order-saga-events
```

In a production split-service architecture, each bounded context would be a separate deployable service with its own database.

---

## 4. Key Features

### Functional features

- Create order
- Get order by ID
- List recent orders
- View saga event timeline
- Simulate payment failure
- Simulate inventory failure
- Simulate shipping failure
- Automatic compensation
- CQRS read model updates
- Multi-tenant isolation

### Platform features

- PostgreSQL-backed command store
- PostgreSQL-backed read projection
- Kafka event choreography
- Saga event audit trail
- Docker Compose local environment
- Stateless Spring Boot API
- Tenant-scoped APIs
- Multiple Kafka consumer groups representing microservices

### Assessment bonus features included

- Choreography saga
- Compensation flow
- CQRS write/read separation
- Event-driven bounded contexts
- Production-readiness guidance
- Cost optimization guidance

---

## 5. Consistency Model

The system intentionally uses a mixed consistency model.

### Strong consistency

- order command is persisted in PostgreSQL
- payment/inventory/shipping state is persisted in command-side tables
- saga events are persisted in `saga_events`

### Eventual consistency

- downstream services react asynchronously to Kafka events
- CQRS read model catches up through event projection
- final order status becomes visible after event processing

### Why this trade-off was chosen

A synchronous distributed transaction across Order, Payment, Inventory, and Shipping would:

- couple all services tightly
- reduce availability
- increase latency
- require distributed locking or 2PC
- make failure recovery difficult

Instead, this system uses local transactions plus events. Each service owns its local state and publishes the next fact in the saga.

---

## 6. Multi-Tenancy Strategy

Tenant isolation is enforced at multiple layers.

### Tenant identification

The API uses the `X-Tenant-ID` header.

### Isolation enforcement

Tenant scope is applied in:

- order creation
- order read model lookup
- saga event history
- payment records
- inventory reservations
- shipment records
- Kafka event payloads

### Why this matters

This prevents accidental cross-tenant leakage in:

- order details
- payment state
- inventory reservation state
- shipment state
- saga timelines
- CQRS projections

---

## 7. API List

### 7.1 Health Check

**Endpoint**

`GET /health`

**Example curl**

```bash
curl http://localhost:8080/health
```

**Example response**

```json
{
  "status": "UP",
  "patterns": {
    "saga": "choreography + compensation",
    "cqrs": "command tables + asynchronous read model projection"
  }
}
```

---

### 7.2 Create Successful Order

**Endpoint**

`POST /orders`

**Headers**

```http
X-Tenant-ID: tenant-a
Content-Type: application/json
```

**Request body**

```json
{
  "userId": "user-1",
  "sku": "SKU-123",
  "quantity": 1,
  "amount": 100.00,
  "paymentMethod": "CARD",
  "shippingAddress": "Delhi NCR"
}
```

**Example curl**

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-a" \
  -d '{
    "userId": "user-1",
    "sku": "SKU-123",
    "quantity": 1,
    "amount": 100.00,
    "paymentMethod": "CARD",
    "shippingAddress": "Delhi NCR"
  }'
```

**Example immediate response**

Because choreography is asynchronous, the immediate response may show an in-progress state:

```json
{
  "orderId": "uuid",
  "tenantId": "tenant-a",
  "userId": "user-1",
  "sku": "SKU-123",
  "quantity": 1,
  "amount": 100.00,
  "orderStatus": "ORDER_CREATED",
  "paymentStatus": "PENDING",
  "inventoryStatus": "PENDING",
  "shipmentStatus": "PENDING"
}
```

After a short delay, querying the order should show:

```json
{
  "orderStatus": "COMPLETED",
  "paymentStatus": "AUTHORIZED",
  "inventoryStatus": "RESERVED",
  "shipmentStatus": "CREATED"
}
```

---

### 7.3 Get Order by ID

**Endpoint**

`GET /orders/{orderId}`

**Headers**

```http
X-Tenant-ID: tenant-a
```

**Example curl**

```bash
curl http://localhost:8080/orders/{orderId} \
  -H "X-Tenant-ID: tenant-a"
```

---

### 7.4 List Recent Orders

**Endpoint**

`GET /orders`

**Headers**

```http
X-Tenant-ID: tenant-a
```

**Example curl**

```bash
curl http://localhost:8080/orders \
  -H "X-Tenant-ID: tenant-a"
```

---

### 7.5 Get Saga Event Timeline

**Endpoint**

`GET /orders/{orderId}/saga-events`

**Headers**

```http
X-Tenant-ID: tenant-a
```

**Example curl**

```bash
curl http://localhost:8080/orders/{orderId}/saga-events \
  -H "X-Tenant-ID: tenant-a"
```

**Example successful timeline**

```json
[
  { "step": "ORDER", "eventType": "ORDER_CREATED" },
  { "step": "PAYMENT", "eventType": "PAYMENT_AUTHORIZED" },
  { "step": "INVENTORY", "eventType": "INVENTORY_RESERVED" },
  { "step": "SHIPPING", "eventType": "SHIPMENT_CREATED" }
]
```

---

### 7.6 Simulate Payment Failure

Use `paymentMethod = FAIL_PAYMENT`.

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-a" \
  -d '{
    "userId": "user-2",
    "sku": "SKU-123",
    "quantity": 1,
    "amount": 100.00,
    "paymentMethod": "FAIL_PAYMENT",
    "shippingAddress": "Delhi NCR"
  }'
```

Expected final state:

```json
{
  "orderStatus": "PAYMENT_FAILED",
  "paymentStatus": "FAILED",
  "inventoryStatus": "PENDING",
  "shipmentStatus": "PENDING"
}
```

---

### 7.7 Simulate Inventory Failure

Use `sku = OUT_OF_STOCK` or `quantity > 100`.

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-a" \
  -d '{
    "userId": "user-3",
    "sku": "OUT_OF_STOCK",
    "quantity": 1,
    "amount": 100.00,
    "paymentMethod": "CARD",
    "shippingAddress": "Delhi NCR"
  }'
```

Expected final state:

```json
{
  "orderStatus": "INVENTORY_FAILED",
  "paymentStatus": "REFUNDED",
  "inventoryStatus": "FAILED",
  "shipmentStatus": "PENDING"
}
```

Expected saga events:

```text
ORDER_CREATED -> PAYMENT_AUTHORIZED -> INVENTORY_FAILED -> PAYMENT_REFUNDED
```

---

### 7.8 Simulate Shipping Failure

Use address containing `FAIL_SHIPPING`.

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-a" \
  -d '{
    "userId": "user-4",
    "sku": "SKU-123",
    "quantity": 1,
    "amount": 100.00,
    "paymentMethod": "CARD",
    "shippingAddress": "FAIL_SHIPPING_ADDRESS"
  }'
```

Expected final state:

```json
{
  "orderStatus": "SHIPPING_FAILED",
  "paymentStatus": "REFUNDED",
  "inventoryStatus": "RELEASED",
  "shipmentStatus": "FAILED"
}
```

Expected saga events:

```text
ORDER_CREATED -> PAYMENT_AUTHORIZED -> INVENTORY_RESERVED -> SHIPMENT_FAILED -> INVENTORY_RELEASED -> PAYMENT_REFUNDED
```

---

## 8. Project Structure

```text
.
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── README.md
├── benchmarks/
│   └── k6-order-smoke.js
└── src/
    ├── main/java/com/example/order/
    │   ├── controller/
    │   ├── dto/
    │   ├── domain/
    │   ├── repository/
    │   └── service/
    └── main/resources/
        └── application.yml
```

---

## 9. How the Code Works

### Controllers

#### OrderController

Handles:

- create order
- get order by ID
- list recent orders
- get saga event timeline
- health check

### Services

#### OrderCommandService

Responsible for:

- creating command-side order row
- initializing CQRS read model
- publishing `ORDER_CREATED`

#### PaymentService

Consumes:

- `ORDER_CREATED`
- `INVENTORY_FAILED`
- `SHIPMENT_FAILED`

Publishes:

- `PAYMENT_AUTHORIZED`
- `PAYMENT_FAILED`
- `PAYMENT_REFUNDED`

#### InventoryService

Consumes:

- `PAYMENT_AUTHORIZED`
- `SHIPMENT_FAILED`

Publishes:

- `INVENTORY_RESERVED`
- `INVENTORY_FAILED`
- `INVENTORY_RELEASED`

#### ShippingService

Consumes:

- `INVENTORY_RESERVED`

Publishes:

- `SHIPMENT_CREATED`
- `SHIPMENT_FAILED`

#### ReadModelService

Consumes all saga events and updates `order_read_model`.

This is the CQRS projection side.

#### SagaLogService

Persists every saga event into `saga_events` and publishes the event to Kafka.

---

## 10. Local Setup and Run Instructions

### Prerequisites

- Docker
- Docker Compose
- Java 17 and Maven only if running outside Docker
- Optional: k6 for load testing

### Run with Docker Compose

```bash
docker compose up -d --build
docker compose logs -f app
```

### To reset state

```bash
docker compose down -v
docker compose up -d --build
```

---

## 11. Full Demo and Testing Steps

### 11.1 Start clean

```bash
docker compose down -v
docker compose up -d --build
docker compose logs -f app
```

### 11.2 Health check

```bash
curl http://localhost:8080/health
```

### 11.3 Create a successful order

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-a" \
  -d '{
    "userId": "user-1",
    "sku": "SKU-123",
    "quantity": 1,
    "amount": 100.00,
    "paymentMethod": "CARD",
    "shippingAddress": "Delhi NCR"
  }'
```

Save the returned order ID:

```bash
ORDER_ID=<paste-id>
```

Wait briefly:

```bash
sleep 2
```

Query order:

```bash
curl http://localhost:8080/orders/$ORDER_ID \
  -H "X-Tenant-ID: tenant-a"
```

### 11.4 Check saga event timeline

```bash
curl http://localhost:8080/orders/$ORDER_ID/saga-events \
  -H "X-Tenant-ID: tenant-a"
```

### 11.5 Verify Kafka topic

```bash
docker compose exec kafka kafka-topics \
  --bootstrap-server kafka:29092 \
  --list
```

Expected:

```text
order-saga-events
```

### 11.6 Consume Kafka events

```bash
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server kafka:29092 \
  --topic order-saga-events \
  --from-beginning \
  --max-messages 10
```

### 11.7 Verify PostgreSQL command tables

```bash
docker compose exec postgres psql -U postgres -d orders \
  -c "select id, tenant_id, status, failure_reason from orders;"
```

```bash
docker compose exec postgres psql -U postgres -d orders \
  -c "select tenant_id, order_id, status from payments;"
```

```bash
docker compose exec postgres psql -U postgres -d orders \
  -c "select tenant_id, order_id, status from inventory_reservations;"
```

```bash
docker compose exec postgres psql -U postgres -d orders \
  -c "select tenant_id, order_id, status from shipments;"
```

### 11.8 Verify CQRS read model

```bash
docker compose exec postgres psql -U postgres -d orders \
  -c "select order_id, order_status, payment_status, inventory_status, shipment_status from order_read_model;"
```

### 11.9 Verify saga event log

```bash
docker compose exec postgres psql -U postgres -d orders \
  -c "select order_id, saga_step, event_type, created_at from saga_events order by created_at;"
```

---

## 12. Production Readiness Analysis

### 12.1 Scalability

#### API layer

The API layer is stateless and can scale horizontally behind a load balancer.

Scale signals:

- CPU usage
- request latency
- order creation rate
- Kafka publish latency
- database connection pool usage

#### Choreography services

In production, split the bounded contexts into independent deployables:

- order-service
- payment-service
- inventory-service
- shipping-service
- order-query-service

Each service can scale independently based on event volume.

#### Kafka scalability

Kafka scales with partitions and consumer groups.

Partition strategy:

```text
key = orderId
```

This keeps events for the same order ordered in one partition.

#### PostgreSQL scalability

Use separate databases per service in production:

- order DB
- payment DB
- inventory DB
- shipping DB
- query DB

This prototype keeps them in one database for local simplicity.

---

### 12.2 Resilience

#### Payment failure

Handled through `PAYMENT_FAILED`, which marks the order failed.

#### Inventory failure

Handled through `INVENTORY_FAILED`, which triggers `PAYMENT_REFUNDED`.

#### Shipping failure

Handled through `SHIPMENT_FAILED`, which triggers:

- `INVENTORY_RELEASED`
- `PAYMENT_REFUNDED`

#### Kafka failure

Production mitigation:

- transactional outbox
- retry publisher
- dead-letter topics
- schema registry
- idempotent consumers

#### Database failure

Production mitigation:

- HA primary/replica
- backups and point-in-time recovery
- connection pool limits
- circuit breakers
- retry with backoff

---

### 12.3 Security

Production system should add:

- JWT authentication
- tenant authorization
- RBAC for admin APIs
- encryption in transit
- encryption at rest
- secrets manager
- request validation
- audit logs
- PCI isolation for payment data
- PII masking in logs

Sensitive fields:

```text
payment method
shipping address
user ID
order amount
```

These should not be logged in raw form in production.

---

### 12.4 Observability

Key metrics:

```text
orders_created_total
saga_events_total
saga_completion_latency_ms
saga_failures_total
payment_failures_total
inventory_failures_total
shipping_failures_total
compensations_total
kafka_publish_failures_total
kafka_consumer_lag
read_model_projection_lag_ms
```

Dashboards:

- order creation rate
- saga success/failure rate
- compensation rate
- event lag by consumer group
- read model freshness
- payment failure rate
- inventory failure rate
- shipping failure rate

Tracing:

```text
POST /orders -> ORDER_CREATED -> PAYMENT_AUTHORIZED -> INVENTORY_RESERVED -> SHIPMENT_CREATED -> COMPLETED
```

Logs should include:

```text
tenantId
orderId
sagaStep
eventType
correlationId
latency
```

---

### 12.5 Performance

Key optimizations:

- keep command API lightweight
- use Kafka partition key as order ID
- make consumers idempotent
- avoid synchronous cross-service calls
- batch read model updates where safe
- index query-side tables
- avoid long database transactions

Recommended read model index:

```sql
CREATE INDEX idx_order_read_model_tenant_updated
ON order_read_model (tenant_id, updated_at DESC);
```

Recommended saga event index:

```sql
CREATE INDEX idx_saga_events_tenant_order_created
ON saga_events (tenant_id, order_id, created_at ASC);
```

---

### 12.6 Operations

Deployment strategy:

- Docker for local
- Kubernetes for production
- Helm chart
- readiness/liveness probes
- rolling deployment
- blue-green deployment
- canary deployment

Operational jobs:

- stuck saga detector
- failed event reprocessor
- DLQ reprocessor
- read model rebuild job
- schema migration job

Rollback strategy:

- event schema versioning
- backward-compatible consumers
- keep previous app version
- deploy consumers before producers when adding fields
- feature flags around new saga steps

---

### 12.7 SLA Considerations

Target SLA:

```text
99.95% API availability
```

Order SLO examples:

```text
99% order commands accepted within 100 ms
99% sagas complete within 5 seconds
Kafka event lag under 10 seconds
Read model freshness under 5 seconds
Compensation success rate above 99.9%
```

To achieve:

- multi-AZ deployment
- replicated Kafka
- HA PostgreSQL
- idempotent consumers
- dead-letter queues
- automated saga repair
- alerting on SLO burn rate

---

## 13. Cost Optimization

### API Layer

- stateless autoscaling
- right-size CPU/memory
- use smaller instances for command/query APIs
- use spot instances for non-critical consumers

### Kafka

- avoid excessive partitions
- use compression
- tune retention period
- compact only topics that need compaction
- use managed Kafka only when operationally justified

### PostgreSQL

- split service databases only when needed
- archive old saga events
- partition large event tables by month
- avoid over-indexing command tables
- keep read model compact

### Consumers

- scale consumers based on lag
- use autoscaling by consumer lag
- batch non-critical projection updates
- avoid over-provisioning each bounded context

### Storage

- keep hot orders in PostgreSQL
- archive old orders to object storage
- keep saga events for audit window only
- export analytics to OLAP store if needed

### Cloud

- reserved instances for steady state
- spot instances for async workers
- separate dev/staging/prod sizes
- log retention policies
- autoscaling groups

---

## 14. Advanced Production Enhancements

### Transactional outbox

Use an outbox table per service:

```text
local DB transaction:
1. update local aggregate
2. insert outbox event

outbox relay:
1. read unpublished events
2. publish to Kafka
3. mark event published
```

This prevents DB commit succeeding but Kafka publish failing.

### Idempotent consumers

Each service should store processed event IDs to avoid double-processing.

### Dead-letter topics

Use DLQ topics:

```text
order-saga-events-dlq
payment-events-dlq
inventory-events-dlq
shipping-events-dlq
```

### Saga timeout handling

A stuck saga detector should find orders that remain in intermediate states too long.

Example:

```text
ORDER_CREATED for more than 10 minutes without PAYMENT_AUTHORIZED
```

### Read model rebuild

CQRS read models should be rebuildable from command-side state or event logs.

---


## 16. Final Summary

This system demonstrates:

- choreography-based saga design
- compensation-based failure recovery
- CQRS read model projection
- Kafka event-driven microservices
- multi-tenant backend architecture
- distributed transaction trade-offs
- production resilience
- cost optimization
