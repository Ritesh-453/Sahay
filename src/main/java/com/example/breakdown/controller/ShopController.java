package com.example.breakdown.controller;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.model.Shop;
import com.example.breakdown.repository.ServiceRequestRepository;
import com.example.breakdown.repository.ShopRepository;
import com.example.breakdown.service.EmailService;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShopController(ShopRepository shopRepository,
                          ServiceRequestRepository requestRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.shopRepository = shopRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
                    if (updated.getBranchesJson() != null) shop.setBranchesJson(updated.getBranchesJson());
                    shopRepository.save(shop);
                    shop.setPassword(null);
                    return ResponseEntity.ok(shop);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/requests")
    public List<ServiceRequest> getRequestsForShop(@RequestParam(required = false) String username) {
        List<ServiceRequest> pending = requestRepository.findByStatus("Pending");
        if (username == null || username.isBlank()) return pending;

        Optional<Shop> shopOpt = shopRepository.findByUsername(username);
        if (shopOpt.isEmpty()) return new ArrayList<>();
        Shop shop = shopOpt.get();
        Long shopId = shop.getId();

        return pending.stream().filter(req -> {
            String stage = req.getAssignmentStage();
            if (stage == null) return true;
            if (stage.equals("OPEN")) return true;
            if (stage.equals("ASSIGNED")) return shopId.equals(req.getAssignedShopId());
            if (stage.equals("BROADCAST")) {
                List<Long> rejected = parseRejectedIds(req.getRejectedShopIds());
                if (rejected.contains(shopId)) return false;
                // Show to all shops if no coordinates set, or within 50km
                if (req.getLatitude() == null || req.getLongitude() == null) return true;
                double dist = getShopMinDistance(shop, req.getLatitude(), req.getLongitude());
                return dist <= 200.0; // expanded radius to 200km for better coverage
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

        // Always try to find next shop regardless of how many have rejected
        double lat = req.getLatitude() != null ? req.getLatitude() : 0.0;
        double lng = req.getLongitude() != null ? req.getLongitude() : 0.0;
        Shop nextShop = findNearestOpenShop(allShops, lat, lng, rejected);
        if (nextShop != null) {
            req.setAssignedShopId(nextShop.getId());
            req.setAssignmentStage("ASSIGNED");
            requestRepository.save(req);
            emailService.sendReassignedNotification(nextShop, req);
            return ResponseEntity.ok(req);
        } else {
            // No more shops available — broadcast to all
            req.setAssignedShopId(null);
            req.setAssignmentStage("BROADCAST");
            System.out.println(">>> ALL SHOPS REJECTED — sending admin email for request ID: " + req.getId());
            emailService.sendAdminNotification(req);
        }

        requestRepository.save(req);

        if ("BROADCAST".equals(req.getAssignmentStage()) && req.getLatitude() != null) {
            for (Shop shop : allShops) {
                if (rejected.contains(shop.getId())) continue;
                if (!isShopOpen(shop)) continue;
                double dist = getShopMinDistance(shop, req.getLatitude(), req.getLongitude());
                if (dist <= 50.0) {
                    emailService.sendBroadcastNotification(shop, req);
                }
            }
        }

        return ResponseEntity.ok(req);
    }

    @PatchMapping("/requests/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return requestRepository.findById(id)
                .map(req -> {
                    if (!"Pending".equals(req.getStatus()))
                        return ResponseEntity.badRequest().body("Request is no longer pending.");
                    req.setStatus("Accepted");
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
            info.put("isOpen", isShopOpen(shop));
            info.put("distanceKm", getShopMinDistance(shop, lat, lng));
            info.put("serverTime", java.time.LocalTime.now().toString());
            result.put(shop.getUsername(), info);
        }
        return result;
    }

    // ========== HELPERS ==========

    private Shop findNearestOpenShop(List<Shop> shops, double lat, double lng, List<Long> excludeIds) {
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

    private boolean isShopOpen(Shop shop) {
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

    private double getShopMinDistance(Shop shop, double lat, double lng) {
        if (shop.getBranchesJson() == null || shop.getBranchesJson().isBlank()) return Double.MAX_VALUE;
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
            return anyValidBranch ? minDist : Double.MAX_VALUE;
        } catch (Exception e) { return Double.MAX_VALUE; }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private List<Long> parseRejectedIds(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}