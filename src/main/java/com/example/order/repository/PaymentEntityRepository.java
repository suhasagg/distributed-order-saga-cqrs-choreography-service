package com.example.order.repository;

import com.example.order.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, String> {
  List<PaymentEntity> findByTenantId(String tenantId);
}
