package com.example.breakdown.repository;

import com.example.breakdown.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByCustomerName(String customerName);

    // For analytics: requests in date range
    List<ServiceRequest> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // For shop dashboard: pending requests only
    List<ServiceRequest> findByStatus(String status);
}