package com.example.breakdown.controller;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.repository.ServiceRequestRepository;
import com.example.breakdown.service.ServiceRequestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class ServiceRequestController {

    private final ServiceRequestRepository requestRepository;
    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestRepository requestRepository,
                                    ServiceRequestService service) {
        this.requestRepository = requestRepository;
        this.service = service;
    }

    // Get all requests (Admin)
    @GetMapping
    public List<ServiceRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    // Get requests by customer name (User sees only their own)
    @GetMapping("/customer/{username}")
    public List<ServiceRequest> getRequestsByUser(@PathVariable String username) {
        return requestRepository.findByCustomerName(username);
    }

    // Create new request
    @PostMapping
    public ServiceRequest createRequest(@RequestBody ServiceRequest request) {
        request.setStatus("Pending");
        request.setCreatedAt(LocalDateTime.now());
        return requestRepository.save(request);
    }

    // ✅ Update status only (used for Completed)
    @PatchMapping("/{id}/status")
    public ServiceRequest updateStatus(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus(body.get("status"));
        return requestRepository.save(req);
    }

    // ✅ NEW: Accept request + save mechanic details in one call
    @PatchMapping("/{id}/accept")
    public ServiceRequest acceptWithMechanic(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        req.setStatus("Accepted");
        req.setMechanicName(body.get("mechanicName"));
        req.setMechanicPhone(body.get("mechanicPhone"));
        req.setMechanicEta(body.get("estimatedArrival"));

        // Mechanic location (optional - may be null if not provided)
        if (body.get("mechanicLatitude") != null && !body.get("mechanicLatitude").isEmpty()) {
            req.setMechanicLatitude(Double.parseDouble(body.get("mechanicLatitude")));
        }
        if (body.get("mechanicLongitude") != null && !body.get("mechanicLongitude").isEmpty()) {
            req.setMechanicLongitude(Double.parseDouble(body.get("mechanicLongitude")));
        }

        return requestRepository.save(req);
    }

    @PatchMapping("/{id}/review")
    public ServiceRequest submitReview(@PathVariable Long id, @RequestBody ServiceRequest body) {
        ServiceRequest req = repository.findById(id).orElseThrow();
        req.setReviewRating(body.getReviewRating());
        req.setReviewComment(body.getReviewComment());
        return repository.save(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Analytics: get list of requests in date range (for table)
    @GetMapping("/analytics")
    public List<ServiceRequest> analytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return requestRepository.findByCreatedAtBetween(from, to);
    }

    // Analytics: get counts per status (for pie chart)
    @GetMapping("/analytics/counts")
    public Map<String, Long> analyticsCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<ServiceRequest> list = requestRepository.findByCreatedAtBetween(from, to);
        return list.stream()
                .collect(Collectors.groupingBy(ServiceRequest::getStatus, Collectors.counting()));
    }
}