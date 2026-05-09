package com.example.breakdown.controller;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.model.Shop;
import com.example.breakdown.repository.ServiceRequestRepository;
import com.example.breakdown.repository.ShopRepository;
import com.example.breakdown.service.EmailService;
import com.example.breakdown.service.ServiceRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class ServiceRequestController {

    private final ServiceRequestRepository requestRepository;
    private final ShopRepository shopRepository;
    private final ServiceRequestService service;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServiceRequestController(ServiceRequestRepository requestRepository,
                                    ShopRepository shopRepository,
                                    ServiceRequestService service,
                                    EmailService emailService) {
        this.requestRepository = requestRepository;
        this.shopRepository = shopRepository;
        this.service = service;
        this.emailService = emailService;
    }

    @GetMapping
    public List<ServiceRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    @GetMapping("/customer/{username}")
    public List<ServiceRequest> getRequestsByUser(@PathVariable String username) {
        return requestRepository.findByCustomerName(username);
    }

    @PostMapping
    public ServiceRequest createRequest(@RequestBody ServiceRequest request) {
        request.setStatus("Pending");
        request.setCreatedAt(LocalDateTime.now());
        request.setRejectedShopIds("");

        System.out.println("=== CREATE REQUEST ===");
        System.out.println("Lat: " + request.getLatitude() + " | Lng: " + request.getLongitude());

        if (request.getLatitude() != null && request.getLongitude() != null) {
            List<Shop> allShops = shopRepository.findAll();
            System.out.println("Total shops: " + allShops.size());
            for (Shop s : allShops) {
                System.out.println("Shop: " + s.getUsername()
                    + " | open: " + isShopOpen(s)
                    + " | dist: " + getShopMinDistance(s, request.getLatitude(), request.getLongitude()));
            }

            Shop nearest = findNearestOpenShop(allShops, request.getLatitude(), request.getLongitude(), new ArrayList<>());
            System.out.println("Nearest: " + (nearest != null ? nearest.getUsername() : "NULL"));

            if (nearest != null) {
                request.setAssignedShopId(nearest.getId());
                request.setAssignmentStage("ASSIGNED");
                ServiceRequest saved = requestRepository.save(request);
                System.out.println("=== SENDING EMAIL TO: " + nearest.getEmail() + " ===");
                emailService.sendNewRequestNotification(nearest, saved);
                return saved;
            } else {
                // No shop found — broadcast so all shops can see it
                request.setAssignedShopId(null);
                request.setAssignmentStage("BROADCAST");
            }
        } else {
            System.out.println("LAT/LNG IS NULL - falling to BROADCAST");
            request.setAssignedShopId(null);
            request.setAssignmentStage("BROADCAST");
        }

        return requestRepository.save(request);
    }

    @PatchMapping("/{id}/status")
    public ServiceRequest updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus(body.get("status"));
        return requestRepository.save(req);
    }

    @PatchMapping("/{id}/accept")
    public ServiceRequest acceptWithMechanic(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus("Accepted");

        // ✅ FIX: set assignedShopId so the shop can see it after accepting
        // RELIABLE - explicit null check outside lambda
        String shopUsername = body.get("shopUsername");
        if (shopUsername != null && !shopUsername.isBlank()) {
            Optional<Shop> shopOpt = shopRepository.findByUsername(shopUsername);
            if (shopOpt.isPresent()) {
                req.setAssignedShopId(shopOpt.get().getId());
            }
        }

        req.setMechanicName(body.get("mechanicName"));
        req.setMechanicPhone(body.get("mechanicPhone"));
        req.setMechanicEta(body.get("estimatedArrival"));
        if (body.get("mechanicLatitude") != null && !body.get("mechanicLatitude").isEmpty())
            req.setMechanicLatitude(Double.parseDouble(body.get("mechanicLatitude")));
        if (body.get("mechanicLongitude") != null && !body.get("mechanicLongitude").isEmpty())
            req.setMechanicLongitude(Double.parseDouble(body.get("mechanicLongitude")));
        return requestRepository.save(req);
    }

    @PatchMapping("/{id}/review")
    public ServiceRequest submitReview(@PathVariable Long id, @RequestBody ServiceRequest body) {
        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setReviewRating(body.getReviewRating());
        req.setReviewComment(body.getReviewComment());
        return requestRepository.save(req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        requestRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analytics")
    public List<ServiceRequest> analytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return requestRepository.findByCreatedAtBetween(from, to);
    }

    @GetMapping("/analytics/counts")
    public Map<String, Long> analyticsCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<ServiceRequest> list = requestRepository.findByCreatedAtBetween(from, to);
        return list.stream().collect(Collectors.groupingBy(ServiceRequest::getStatus, Collectors.counting()));
    }

    // ========== SHARED HELPERS ==========

    public Shop findNearestOpenShop(List<Shop> shops, double lat, double lng, List<Long> excludeIds) {
        Shop nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Shop shop : shops) {
            if (excludeIds.contains(shop.getId())) continue;
            if (!isShopOpen(shop)) continue;
            double dist = getShopMinDistance(shop, lat, lng);
            if (dist < minDist) { minDist = dist; nearest = shop; }
        }
        return nearest;
    }

    public boolean isShopOpen(Shop shop) {
        if (shop.getOpeningTime() == null || shop.getClosingTime() == null) return true;
        try {
            String openStr = shop.getOpeningTime().trim();
            String closeStr = shop.getClosingTime().trim();
            if (openStr.length() > 5) openStr = openStr.substring(0, 5);
            if (closeStr.length() > 5) closeStr = closeStr.substring(0, 5);
            LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"));
            LocalTime open = LocalTime.parse(openStr);
            LocalTime close = LocalTime.parse(closeStr);
            return !now.isBefore(open) && !now.isAfter(close);
        } catch (Exception e) { return true; }
    }

    public double getShopMinDistance(Shop shop, double lat, double lng) {
        if (shop.getBranchesJson() == null || shop.getBranchesJson().isBlank()) {
            // FIX: No branches configured — treat shop as local (distance 0) so it gets assigned
            return 0.0;
        }
        try {
            List<Map<String, Object>> branches = objectMapper.readValue(shop.getBranchesJson(), List.class);
            double minDist = Double.MAX_VALUE;
            boolean anyValidBranch = false;
            for (Map<String, Object> branch : branches) {
                Object latObj = branch.get("lat");
                Object lngObj = branch.get("lng");
                if (latObj == null || lngObj == null) continue;
                String latStr = latObj.toString().trim();
                String lngStr = lngObj.toString().trim();
                if (latStr.isEmpty() || lngStr.isEmpty()) continue;
                double bLat = Double.parseDouble(latStr);
                double bLng = Double.parseDouble(lngStr);
                if (bLat == 0.0 && bLng == 0.0) continue;
                anyValidBranch = true;
                double dist = haversine(lat, lng, bLat, bLng);
                if (dist < minDist) minDist = dist;
            }
            // FIX: If branches exist but none have valid coordinates — treat as local (distance 0)
            return anyValidBranch ? minDist : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    public double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}