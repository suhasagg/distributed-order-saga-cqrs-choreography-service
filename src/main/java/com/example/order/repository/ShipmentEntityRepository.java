package com.example.order.repository;

import com.example.order.domain.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShipmentEntityRepository extends JpaRepository<ShipmentEntity, String> {
  List<ShipmentEntity> findByTenantId(String tenantId);
}
