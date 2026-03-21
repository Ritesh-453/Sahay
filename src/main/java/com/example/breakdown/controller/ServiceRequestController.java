package com.example.breakdown.controller;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.repository.ServiceRequestRepository;
import com.example.breakdown.service.ServiceRequestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    // Get requests by customer username
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

    // Update status only (used for Completed)
    @PatchMapping("/{id}/status")
    public ServiceRequest updateStatus(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus(body.get("status"));
        return requestRepository.save(req);
    }

    // Accept request + save mechanic details
    @PatchMapping("/{id}/accept")
    public ServiceRequest acceptWithMechanic(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus("Accepted");
        req.setMechanicName(body.get("mechanicName"));
        req.setMechanicPhone(body.get("mechanicPhone"));
        req.setMechanicEta(body.get("estimatedArrival"));
        if (body.get("mechanicLatitude") != null && !body.get("mechanicLatitude").isEmpty()) {
            req.setMechanicLatitude(Double.parseDouble(body.get("mechanicLatitude")));
        }
        if (body.get("mechanicLongitude") != null && !body.get("mechanicLongitude").isEmpty()) {
            req.setMechanicLongitude(Double.parseDouble(body.get("mechanicLongitude")));
        }
        return requestRepository.save(req);
    }

    // Submit review (Customer)
    @PatchMapping("/{id}/review")
    public ServiceRequest submitReview(@PathVariable Long id,
                                       @RequestBody ServiceRequest body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setReviewRating(body.getReviewRating());
        req.setReviewComment(body.getReviewComment());
        return requestRepository.save(req);
    }

    // Delete request (Customer — Pending only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        requestRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Analytics: get list of requests in date range
    @GetMapping("/analytics")
    public List<ServiceRequest> analytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return requestRepository.findByCreatedAtBetween(from, to);
    }

    // Analytics: get counts per status
    @GetMapping("/analytics/counts")
    public Map<String, Long> analyticsCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<ServiceRequest> list = requestRepository.findByCreatedAtBetween(from, to);
        return list.stream()
                .collect(Collectors.groupingBy(ServiceRequest::getStatus, Collectors.counting()));
    }
}