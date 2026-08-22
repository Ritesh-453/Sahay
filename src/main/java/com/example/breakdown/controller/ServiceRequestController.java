package com.example.breakdown.controller;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.model.Shop;
import com.example.breakdown.repository.ServiceRequestRepository;
import com.example.breakdown.repository.ShopRepository;
import com.example.breakdown.service.EmailService;
import com.example.breakdown.service.ServiceRequestService;
import com.example.breakdown.service.ShopService;
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
    private final ShopService shopService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServiceRequestController(ServiceRequestRepository requestRepository,
                                    ShopRepository shopRepository,
                                    ServiceRequestService service,
                                    EmailService emailService,
                                    ShopService shopService) {
        this.requestRepository = requestRepository;
        this.shopRepository = shopRepository;
        this.service = service;
        this.emailService = emailService;
        this.shopService = shopService;
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
                    + " | open: " + shopService.isShopOpen(s)
                    + " | dist: " + shopService.getShopMinDistance(s, request.getLatitude(), request.getLongitude()));
            }

            Shop nearest = shopService.findNearestOpenShop(allShops, request.getLatitude(), request.getLongitude(), new ArrayList<>());
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

    @PatchMapping("/{id}/reject")
    public ServiceRequest rejectRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        ServiceRequest req = requestRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Request not found"));

        String shopUsername = body.get("shopUsername");

        if (shopUsername == null || shopUsername.isBlank()) {
            throw new RuntimeException("Shop username is required");
        }

        Shop rejectingShop = shopRepository
                .findByUsername(shopUsername)
                .orElseThrow(() ->
                        new RuntimeException("Shop not found"));

        // ---------------------------------
        // Add current shop to rejected list
        // ---------------------------------

        List<Long> rejectedIds = new ArrayList<>();

        if (req.getRejectedShopIds() != null &&
                !req.getRejectedShopIds().isBlank()) {

            rejectedIds = Arrays.stream(
                            req.getRejectedShopIds().split(","))
                    .filter(s -> !s.isBlank())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }

        if (!rejectedIds.contains(rejectingShop.getId())) {
            rejectedIds.add(rejectingShop.getId());
        }

        req.setRejectedShopIds(
                rejectedIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
        );

        // ---------------------------------
        // Find next nearest eligible shop
        // ---------------------------------

        List<Shop> allShops = shopRepository.findAll();

        Shop nextShop = shopService.findNearestOpenShop(
                allShops,
                req.getLatitude(),
                req.getLongitude(),
                rejectedIds
        );

        if (nextShop != null) {

            // Assign request to next shop
            req.setAssignedShopId(nextShop.getId());
            req.setAssignmentStage("ASSIGNED");
            req.setLastAssignedAt(LocalDateTime.now());

            ServiceRequest saved =
                    requestRepository.save(req);

            // Send email to next shop
            emailService.sendReassignedNotification(
                    nextShop,
                    saved
            );

            System.out.println(
                    ">>> Request " + saved.getId()
                            + " reassigned to "
                            + nextShop.getUsername()
            );

            return saved;
        }

        // ---------------------------------
        // No more individual shops available
        // Broadcast to remaining shops
        // ---------------------------------

        final List<Long> finalRejectedIds = rejectedIds;

        List<Shop> broadcastShops = allShops.stream()
                .filter(shop -> !finalRejectedIds.contains(shop.getId()))
                .filter(shop -> shopService.isShopOpen(shop))
                .collect(Collectors.toList());

        if (!broadcastShops.isEmpty()) {

            req.setAssignedShopId(null);
            req.setAssignmentStage("BROADCAST");
            req.setLastAssignedAt(LocalDateTime.now());

            ServiceRequest saved =
                    requestRepository.save(req);

            // Send email to every broadcast shop
            for (Shop shop : broadcastShops) {

                emailService.sendBroadcastNotification(
                        shop,
                        saved
                );

                System.out.println(
                        ">>> Broadcast email sent to "
                                + shop.getEmail()
                );
            }

            return saved;
        }

        // ---------------------------------
        // Nobody remains -> notify admin
        // ---------------------------------

        req.setAssignedShopId(null);
        req.setAssignmentStage("OPEN");
        req.setAdminNotified(true);

        ServiceRequest saved =
                requestRepository.save(req);

        emailService.sendAdminNotification(saved);

        System.out.println(
                ">>> All shops rejected. Admin notified."
        );

        return saved;
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
    // Removed because these are now in ShopService
}