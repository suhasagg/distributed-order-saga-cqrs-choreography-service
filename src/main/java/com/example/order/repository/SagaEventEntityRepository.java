package com.example.order.repository;

import com.example.order.domain.SagaEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SagaEventEntityRepository extends JpaRepository<SagaEventEntity, String> {
  List<SagaEventEntity> findByTenantIdAndOrderIdOrderByCreatedAtAsc(String tenantId, String orderId);
}
