package com.example.breakdown.controller;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.model.Shop;
import com.example.breakdown.repository.ServiceRequestRepository;
import com.example.breakdown.repository.ShopRepository;
import com.example.breakdown.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/track")
@CrossOrigin(origins = "*")
public class TrackingController {

    private final ServiceRequestRepository requestRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public TrackingController(ServiceRequestRepository requestRepository,
                               ShopRepository shopRepository,
                               UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    // Mechanic calls this every second to update their location
    @PatchMapping("/{requestId}/location")
    public ResponseEntity<?> updateMechanicLocation(
            @PathVariable Long requestId,
            @RequestBody Map<String, Double> body) {

        ServiceRequest req = requestRepository.findById(requestId).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();

        req.setMechanicLatitude(body.get("lat"));
        req.setMechanicLongitude(body.get("lng"));
        requestRepository.save(req);
        return ResponseEntity.ok().build();
    }

    // Customer / Admin / Shop owner calls this to get mechanic location
    // Access: customerName OR assignedShopId owner OR admin role
    @GetMapping("/{requestId}/location")
    public ResponseEntity<?> getMechanicLocation(
            @PathVariable Long requestId,
            @RequestParam String requester,   // username of the person asking
            @RequestParam String role) {       // "CUSTOMER", "SHOP", "ADMIN"

        ServiceRequest req = requestRepository.findById(requestId).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();

        boolean allowed = false;

        if ("ADMIN".equals(role)) {
            // Admin can always see
            allowed = userRepository.findByUsername(requester)
                    .map(u -> "ADMIN".equals(u.getRole()))
                    .orElse(false);

        } else if ("CUSTOMER".equals(role)) {
            // Only the customer who made the request
            allowed = requester.equals(req.getCustomerName());

        } else if ("SHOP".equals(role)) {
            // Allow if this shop is the assigned shop, OR if assignedShopId is null
            // but the request is Accepted (handles requests accepted before assignedShopId fix)
            Optional<Shop> shopOpt = shopRepository.findByUsername(requester);
            if (shopOpt.isPresent()) {
                Long shopId = shopOpt.get().getId();
                if (shopId.equals(req.getAssignedShopId())) {
                    allowed = true;
                } else if (req.getAssignedShopId() == null && "Accepted".equals(req.getStatus())) {
                    // Legacy: assignedShopId not set, auto-fix it now
                    req.setAssignedShopId(shopId);
                    requestRepository.save(req);
                    allowed = true;
                }
            }
        }

        if (!allowed) return ResponseEntity.status(403).body("Access denied");

        if (req.getMechanicLatitude() == null || req.getMechanicLongitude() == null) {
            return ResponseEntity.ok(Map.of(
                "available", false,
                "message", "Mechanic location not shared yet"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "available", true,
            "lat", req.getMechanicLatitude(),
            "lng", req.getMechanicLongitude(),
            "mechanicName", req.getMechanicName() != null ? req.getMechanicName() : "",
            "mechanicPhone", req.getMechanicPhone() != null ? req.getMechanicPhone() : "",
            "customerLat", req.getLatitude() != null ? req.getLatitude() : 0,
            "customerLng", req.getLongitude() != null ? req.getLongitude() : 0
        ));
    }
}