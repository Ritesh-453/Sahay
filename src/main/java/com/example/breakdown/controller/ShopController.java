package com.example.breakdown.controller;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.model.Shop;
import com.example.breakdown.repository.ServiceRequestRepository;
import com.example.breakdown.repository.ShopRepository;
import com.example.breakdown.service.EmailService;
import com.example.breakdown.service.ShopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shop")
@CrossOrigin(origins = "*")
public class ShopController {

    private final ShopRepository shopRepository;
    private final ServiceRequestRepository requestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ShopService shopService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShopController(ShopRepository shopRepository,
                          ServiceRequestRepository requestRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          ShopService shopService) {
        this.shopRepository = shopRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.shopService = shopService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Shop shop) {
        if (shop.getUsername() == null || shop.getPassword() == null ||
            shop.getUsername().isBlank() || shop.getPassword().isBlank())
            return ResponseEntity.badRequest().body("Username and password are required.");
        if (shopRepository.findByUsername(shop.getUsername()).isPresent())
            return ResponseEntity.badRequest().body("Username already exists!");
        if (shop.getShopName() == null || shop.getShopName().isBlank())
            return ResponseEntity.badRequest().body("Shop name is required.");
        if (shop.getOwnerName() == null || shop.getOwnerName().isBlank())
            return ResponseEntity.badRequest().body("Owner name is required.");
        if (shop.getPhone() == null || shop.getPhone().isBlank())
            return ResponseEntity.badRequest().body("Phone number is required.");
        if (!isValidLocation(shop.getBranchesJson()))
            return ResponseEntity.badRequest().body("At least one valid branch location (lat/lng) is required.");
        shop.setPassword(passwordEncoder.encode(shop.getPassword()));
        shop.setRole("SHOP_OWNER");
        shopRepository.save(shop);
        return ResponseEntity.ok("Shop registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> data) {
        return shopRepository.findByUsername(data.get("username"))
                .filter(shop -> passwordEncoder.matches(data.get("password"), shop.getPassword()))
                .map(shop -> { shop.setPassword(null); return ResponseEntity.ok(shop); })
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getProfile(@PathVariable String username) {
        return shopRepository.findByUsername(username)
                .map(shop -> { shop.setPassword(null); return ResponseEntity.ok(shop); })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile/{username}")
    public ResponseEntity<?> updateProfile(@PathVariable String username, @RequestBody Shop updated) {
        return shopRepository.findByUsername(username)
                .map(shop -> {
                    if (updated.getShopName() != null) shop.setShopName(updated.getShopName());
                    if (updated.getOwnerName() != null) shop.setOwnerName(updated.getOwnerName());
                    if (updated.getEmail() != null) shop.setEmail(updated.getEmail());
                    if (updated.getPhone() != null) shop.setPhone(updated.getPhone());
                    if (updated.getOpeningTime() != null) shop.setOpeningTime(updated.getOpeningTime());
                    if (updated.getClosingTime() != null) shop.setClosingTime(updated.getClosingTime());
                    if (updated.getBranchesJson() != null) {
                        if (!isValidLocation(updated.getBranchesJson()))
                            return ResponseEntity.badRequest().body("At least one valid branch location (lat/lng) is required.");
                        shop.setBranchesJson(updated.getBranchesJson());
                    }
                    shopRepository.save(shop);
                    shop.setPassword(null);
                    return ResponseEntity.ok(shop);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/requests")
    public List<ServiceRequest> getRequestsForShop(@RequestParam(required = false) String username) {
        if (username == null || username.isBlank()) return requestRepository.findByStatus("Pending");

        Optional<Shop> shopOpt = shopRepository.findByUsername(username);
        if (shopOpt.isEmpty()) return new ArrayList<>();
        Shop shop = shopOpt.get();
        Long shopId = shop.getId();

        List<ServiceRequest> all = requestRepository.findAll();
        return all.stream().filter(req -> {
            String status = req.getStatus();

            if ("Accepted".equals(status) || "Completed".equals(status)) {
                return req.getAssignedShopId() == null || shopId.equals(req.getAssignedShopId());
            }

            if (!"Pending".equals(status)) return false;

            String stage = req.getAssignmentStage();
            if (stage == null || stage.equals("OPEN")) return true;
            if (stage.equals("ASSIGNED")) return shopId.equals(req.getAssignedShopId());
            if (stage.equals("BROADCAST")) {
                List<Long> rejected = parseRejectedIds(req.getRejectedShopIds());
                if (rejected.contains(shopId)) return false;
                if (req.getLatitude() == null || req.getLongitude() == null) return true;
                double dist = shopService.getShopMinDistance(shop, req.getLatitude(), req.getLongitude());
                return dist <= 200.0;
            }
            return false;
        }).collect(Collectors.toList());
    }

    @GetMapping("/test-email")
    public ResponseEntity<?> testEmail() {
        try {
            ServiceRequest dummy = new ServiceRequest();
            dummy.setCustomerName("Test Customer");
            dummy.setVehicleNumber("TEST-123");
            dummy.setIssue("Test Issue");
            emailService.sendAdminNotification(dummy);
            return ResponseEntity.ok("Admin email sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed: " + e.getMessage());
        }
    }

    @PatchMapping("/requests/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null) return ResponseEntity.badRequest().body("Username required.");

        final long rejectingShopId;
        if (username.equals("ADMIN_FORWARD")) {
            rejectingShopId = -1L;
        } else {
            Optional<Shop> shopOpt = shopRepository.findByUsername(username);
            if (shopOpt.isEmpty()) return ResponseEntity.notFound().build();
            rejectingShopId = shopOpt.get().getId();
        }

        ServiceRequest req = requestRepository.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
        if (!"Pending".equals(req.getStatus()))
            return ResponseEntity.badRequest().body("Request is no longer pending.");

        List<Long> rejected = parseRejectedIds(req.getRejectedShopIds());
        if (!rejected.contains(rejectingShopId)) rejected.add(rejectingShopId);
        req.setRejectedShopIds(rejected.stream().map(String::valueOf).collect(Collectors.joining(",")));

        List<Shop> allShops = shopRepository.findAll();
        double lat = req.getLatitude() != null ? req.getLatitude() : 0.0;
        double lng = req.getLongitude() != null ? req.getLongitude() : 0.0;

        // Step 1: Try to find next nearest shop
        Shop nextShop = shopService.findNearestOpenShop(allShops, lat, lng, rejected);
        if (nextShop != null) {
            req.setAssignedShopId(nextShop.getId());
            req.setAssignmentStage("ASSIGNED");
            req.setLastAssignedAt(java.time.LocalDateTime.now());
            req.setAdminNotified(false);
            requestRepository.save(req);
            emailService.sendReassignedNotification(nextShop, req);
            return ResponseEntity.ok(req);
        }

        // Step 2: No next shop — broadcast to remaining open shops
        final List<Long> finalRejected = rejected;
        List<Shop> broadcastShops = allShops.stream()
                .filter(shop -> !finalRejected.contains(shop.getId()))
                .filter(shop -> shopService.isShopOpen(shop))
                .collect(Collectors.toList());

        if (!broadcastShops.isEmpty()) {
            req.setAssignedShopId(null);
            req.setAssignmentStage("BROADCAST");
            req.setLastAssignedAt(java.time.LocalDateTime.now());
            requestRepository.save(req);
            for (Shop shop : broadcastShops) {
                emailService.sendBroadcastNotification(shop, req);
                System.out.println(">>> Broadcast email sent to " + shop.getEmail());
            }
            return ResponseEntity.ok(req);
        }

        // Step 3: Nobody remains — notify admin
        req.setAssignedShopId(null);
        req.setAssignmentStage("OPEN");
        req.setAdminNotified(true);
        requestRepository.save(req);
        emailService.sendAdminNotification(req);
        System.out.println(">>> All shops rejected. Admin notified.");

        return ResponseEntity.ok(req);
    }

    @PatchMapping("/requests/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String username = body.get("shopUsername");
        return requestRepository.findById(id)
                .map(req -> {
                    if (!"Pending".equals(req.getStatus()))
                        return ResponseEntity.badRequest().body("Request is no longer pending.");
                    req.setStatus("Accepted");
                    if (username != null && !username.isBlank()) {
                        shopRepository.findByUsername(username)
                                .ifPresent(shop -> req.setAssignedShopId(shop.getId()));
                    }
                    req.setMechanicName(body.get("mechanicName"));
                    req.setMechanicPhone(body.get("mechanicPhone"));
                    req.setMechanicEta(body.get("estimatedArrival"));
                    if (body.get("mechanicLatitude") != null && !body.get("mechanicLatitude").isEmpty())
                        req.setMechanicLatitude(Double.parseDouble(body.get("mechanicLatitude")));
                    if (body.get("mechanicLongitude") != null && !body.get("mechanicLongitude").isEmpty())
                        req.setMechanicLongitude(Double.parseDouble(body.get("mechanicLongitude")));
                    requestRepository.save(req);
                    return ResponseEntity.ok(req);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public List<Shop> getAllShops() {
        List<Shop> shops = shopRepository.findAll();
        shops.forEach(s -> s.setPassword(null));
        return shops;
    }

    @GetMapping("/debug-nearest")
    public Map<String, Object> debugNearest(@RequestParam double lat, @RequestParam double lng) {
        List<Shop> allShops = shopRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Shop shop : allShops) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", shop.getId());
            info.put("username", shop.getUsername());
            info.put("openingTime", shop.getOpeningTime());
            info.put("closingTime", shop.getClosingTime());
            info.put("branchesJson", shop.getBranchesJson());
            info.put("isOpen", shopService.isShopOpen(shop));
            info.put("distanceKm", shopService.getShopMinDistance(shop, lat, lng));
            info.put("serverTime", java.time.LocalTime.now().toString());
            result.put(shop.getUsername(), info);
        }
        return result;
    }

    // ========== HELPERS ==========

    private List<Long> parseRejectedIds(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private boolean isValidLocation(String branchesJson) {
        if (branchesJson == null || branchesJson.isBlank()) return false;
        try {
            List<Map<String, Object>> branches = objectMapper.readValue(branchesJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> branch : branches) {
                Object latObj = branch.get("lat");
                Object lngObj = branch.get("lng");
                if (latObj instanceof Number && lngObj instanceof Number) {
                    double lat = ((Number) latObj).doubleValue();
                    double lng = ((Number) lngObj).doubleValue();
                    if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}