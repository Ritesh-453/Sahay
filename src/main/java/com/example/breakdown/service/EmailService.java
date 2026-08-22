package com.example.breakdown.service;

import com.example.breakdown.model.ServiceRequest;
import com.example.breakdown.model.Shop;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;

    @Value("${resend.api.key}")
    private String resendApiKey;

    private static final String ADMIN_EMAIL = "riteshtp2580@gmail.com";

    public EmailService(@Value("${resend.api.key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("Sahay <onboarding@resend.dev>")
                    .to(to)
                    .subject(subject)
                    .html("<pre style='font-family:Arial; white-space:pre-wrap;'>"
                            + text + "</pre>")
                    .build();

            resend.emails().send(params);

            System.out.println(">>> Email sent successfully to " + to);

        } catch (Exception e) {
            // DO NOT stop the service request if email fails
            System.err.println(">>> Email failed to " + to);
            System.err.println(">>> Reason: " + e.getMessage());
        }
    }

    public void sendNewRequestNotification(Shop shop, ServiceRequest req) {

        if (shop.getEmail() == null || shop.getEmail().isBlank()) return;

        String text =
                "Hello " + shop.getOwnerName() + ",\n\n" +
                "A new breakdown request has been assigned to your shop!\n\n" +
                "--- Request Details ---\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " +
                (req.getLatitude() != null
                        ? req.getLatitude() + ", " + req.getLongitude()
                        : "N/A") +
                "\n\nPlease log in to your Sahay dashboard to accept or reject this request.\n\n" +
                "- Sahay Team";

        sendEmail(
                shop.getEmail(),
                "New Service Request - Sahay",
                text
        );
    }

    public void sendReassignedNotification(Shop shop, ServiceRequest req) {

        if (shop.getEmail() == null || shop.getEmail().isBlank()) return;

        String text =
                "Hello " + shop.getOwnerName() + ",\n\n" +
                "A breakdown request has been forwarded to your shop as the previous shop was unavailable.\n\n" +
                "--- Request Details ---\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " +
                (req.getLatitude() != null
                        ? req.getLatitude() + ", " + req.getLongitude()
                        : "N/A") +
                "\n\nPlease log in to your Sahay dashboard.\n\n" +
                "- Sahay Team";

        sendEmail(
                shop.getEmail(),
                "Service Request Forwarded - Sahay",
                text
        );
    }

    public void sendBroadcastNotification(Shop shop, ServiceRequest req) {

        if (shop.getEmail() == null || shop.getEmail().isBlank()) return;

        String text =
                "Hello " + shop.getOwnerName() + ",\n\n" +
                "A breakdown request near your area is looking for an available shop!\n\n" +
                "--- Request Details ---\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " +
                (req.getLatitude() != null
                        ? req.getLatitude() + ", " + req.getLongitude()
                        : "N/A") +
                "\n\nPlease log in to your Sahay dashboard to accept this request.\n\n" +
                "- Sahay Team";

        sendEmail(
                shop.getEmail(),
                "Nearby Service Request Available - Sahay",
                text
        );
    }

    public void sendAdminNotification(ServiceRequest req) {

        String text =
                "Admin Alert,\n\n" +
                "All available shops have rejected the following request. Manual intervention required.\n\n" +
                "--- Request Details ---\n" +
                "Request ID    : " + req.getId() + "\n" +
                "Customer Name : " + req.getCustomerName() + "\n" +
                "Phone         : " + (req.getPhone() != null ? req.getPhone() : "N/A") + "\n" +
                "Vehicle       : " + req.getVehicleNumber() + "\n" +
                "Issue         : " + req.getIssue() + "\n" +
                "Location      : " +
                (req.getLatitude() != null
                        ? req.getLatitude() + ", " + req.getLongitude()
                        : "N/A") +
                "\n\n- Sahay System";

        sendEmail(
                ADMIN_EMAIL,
                "URGENT: No Shop Available - Sahay",
                text
        );
    }
}