package com.example.breakdown.service;

import com.example.breakdown.model.Shop;
import com.example.breakdown.repository.ShopRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    public Shop findNearestOpenShop(List<Shop> shops, double lat, double lng, List<Long> excludeIds) {
        Shop nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Shop shop : shops) {
            if (excludeIds.contains(shop.getId())) continue;
            if (!isShopOpen(shop)) continue;
            double dist = getShopMinDistance(shop, lat, lng);
            if (dist < minDist) { 
                minDist = dist; 
                nearest = shop; 
            }
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
        } catch (Exception e) { 
            return true; 
        }
    }

    public double getShopMinDistance(Shop shop, double lat, double lng) {
        if (shop.getBranchesJson() == null || shop.getBranchesJson().isBlank()) {
            return Double.MAX_VALUE;
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
            return anyValidBranch ? minDist : Double.MAX_VALUE;
        } catch (Exception e) { 
            return Double.MAX_VALUE; 
        }
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
