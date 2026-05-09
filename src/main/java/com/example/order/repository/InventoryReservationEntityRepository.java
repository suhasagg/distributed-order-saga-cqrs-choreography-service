package com.example.order.repository;

import com.example.order.domain.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryReservationEntityRepository extends JpaRepository<InventoryReservationEntity, String> {
  List<InventoryReservationEntity> findByTenantId(String tenantId);
}
