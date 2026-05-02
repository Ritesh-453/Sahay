package com.example.breakdown.service;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository repo;

    public ServiceRequestService(ServiceRequestRepository repo) {
        this.repo = repo;
    }

    public List<ServiceRequest> getAllRequests() {
        return repo.findAll();
    }

    public Map<String, Long> getAnalytics(LocalDateTime start, LocalDateTime end) {

        List<ServiceRequest> requests = repo.findByCreatedAtBetween(start, end);

        Map<String, Long> stats = new HashMap<>();
        stats.put("Pending", 0L);
        stats.put("Accepted", 0L);
        stats.put("Completed", 0L);

        for (ServiceRequest r : requests) {
            stats.put(r.getStatus(),
                    stats.getOrDefault(r.getStatus(), 0L) + 1);
        }

        return stats;
    }
}