package com.example.order.repository;

import com.example.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderEntityRepository extends JpaRepository<OrderEntity, String> {
  Optional<OrderEntity> findByTenantIdAndId(String tenantId, String id);
  List<OrderEntity> findTop50ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
