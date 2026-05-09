package com.example.order.repository;

import com.example.order.domain.OrderProjectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderProjectionEntityRepository extends JpaRepository<OrderProjectionEntity, String> {
  Optional<OrderProjectionEntity> findByTenantIdAndOrderId(String tenantId, String orderId);
  List<OrderProjectionEntity> findTop50ByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
