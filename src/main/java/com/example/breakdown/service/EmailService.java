package com.example.breakdown.service;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.model.Shop;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private static final String ADMIN_EMAIL = "riteshtp2580@gmail.com";

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendNewRequestNotification(Shop shop, ServiceRequest req) {
        if (shop.getEmail() == null || shop.getEmail().isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(shop.getEmail());
            msg.setSubject("New Service Request - RoadAssist");
            msg.setText(
                "Hello " + shop.getOwnerName() + ",\n\n" +
                "A new breakdown request has been assigned to your shop!\n\n" +
                "--- Request Details ---\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " + (req.getLatitude() != null ? req.getLatitude() + ", " + req.getLongitude() : "N/A") + "\n\n" +
                "Please log in to your RoadAssist dashboard to accept or reject this request.\n\n" +
                "- RoadAssist Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + shop.getEmail() + ": " + e.getMessage());
        }
    }

    public void sendReassignedNotification(Shop shop, ServiceRequest req) {
        if (shop.getEmail() == null || shop.getEmail().isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(shop.getEmail());
            msg.setSubject("Service Request Forwarded to You - RoadAssist");
            msg.setText(
                "Hello " + shop.getOwnerName() + ",\n\n" +
                "A breakdown request has been forwarded to your shop as the previous shop was unavailable.\n\n" +
                "--- Request Details ---\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " + (req.getLatitude() != null ? req.getLatitude() + ", " + req.getLongitude() : "N/A") + "\n\n" +
                "Please log in to your RoadAssist dashboard to accept or reject this request.\n\n" +
                "- RoadAssist Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + shop.getEmail() + ": " + e.getMessage());
        }
    }

    public void sendBroadcastNotification(Shop shop, ServiceRequest req) {
        if (shop.getEmail() == null || shop.getEmail().isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(shop.getEmail());
            msg.setSubject("Nearby Service Request Available - RoadAssist");
            msg.setText(
                "Hello " + shop.getOwnerName() + ",\n\n" +
                "A breakdown request near your area is looking for an available shop!\n\n" +
                "--- Request Details ---\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " + (req.getLatitude() != null ? req.getLatitude() + ", " + req.getLongitude() : "N/A") + "\n\n" +
                "Please log in to your RoadAssist dashboard to accept this request.\n\n" +
                "- RoadAssist Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + shop.getEmail() + ": " + e.getMessage());
        }
    }

    public void sendAdminNotification(ServiceRequest req) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(ADMIN_EMAIL);
            msg.setSubject("URGENT: No Shop Available for Request - RoadAssist");
            msg.setText(
                "Admin Alert,\n\n" +
                "All available shops have rejected the following request. Manual intervention required.\n\n" +
                "--- Request Details ---\n" +
                "Request ID    : " + req.getId() + "\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " + (req.getLatitude() != null ? req.getLatitude() + ", " + req.getLongitude() : "N/A") + "\n\n" +
                "- RoadAssist System"
            );
            mailSender.send(msg);
            System.out.println(">>> Admin email sent successfully to " + ADMIN_EMAIL);
        } catch (Exception e) {
            System.err.println("Failed to send admin email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}