package com.example.breakdown.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String phone;
    private String vehicleNumber;
    private String issue;
    private String status;
    private LocalDateTime createdAt;

    private Double latitude;
    private Double longitude;

    // ✅ NEW: Mechanic details
    private String mechanicName;
    private String mechanicPhone;
    private String mechanicEta;
    private Double mechanicLatitude;
    private Double mechanicLongitude;

    // Vehicle image (base64)
    @Column(columnDefinition = "LONGTEXT")
    private String vehicleImage;

    // Review
    private Integer reviewRating;

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "Pending";
    }

    // Getters & Setters
    public Long getId() { return id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getMechanicName() { return mechanicName; }
    public void setMechanicName(String mechanicName) { this.mechanicName = mechanicName; }

    public String getMechanicPhone() { return mechanicPhone; }
    public void setMechanicPhone(String mechanicPhone) { this.mechanicPhone = mechanicPhone; }

    public String getMechanicEta() { return mechanicEta; }
    public void setMechanicEta(String mechanicEta) { this.mechanicEta = mechanicEta; }

    public Double getMechanicLatitude() { return mechanicLatitude; }
    public void setMechanicLatitude(Double mechanicLatitude) { this.mechanicLatitude = mechanicLatitude; }

    public Double getMechanicLongitude() { return mechanicLongitude; }
    public void setMechanicLongitude(Double mechanicLongitude) { this.mechanicLongitude = mechanicLongitude; }

    public String getVehicleImage() { return vehicleImage; }
    public void setVehicleImage(String vehicleImage) { this.vehicleImage = vehicleImage; }

    public Integer getReviewRating() { return reviewRating; }
    public void setReviewRating(Integer reviewRating) { this.reviewRating = reviewRating; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}