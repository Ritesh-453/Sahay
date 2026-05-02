package com.example.breakdown.model;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "SHOP")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner / login info
    @Column(unique = true)
    private String username;

    private String password;

    private String role; // SHOP_OWNER

    // Shop details
    private String shopName;
    private String ownerName;
    private String email;
    private String phone;

    // Opening / closing times (stored as strings like "09:00", "21:00")
    private String openingTime;
    private String closingTime;

    // Branches stored as JSON string: [{name, address, lat, lng}]
    @Column(columnDefinition = "TEXT")
    private String branchesJson;

    // Getters & Setters
    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }

    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }

    public String getBranchesJson() { return branchesJson; }
    public void setBranchesJson(String branchesJson) { this.branchesJson = branchesJson; }
}