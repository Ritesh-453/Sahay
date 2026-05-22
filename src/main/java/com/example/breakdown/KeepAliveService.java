package com.example.breakdown;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveService.class);
    private static final String APP_URL = "https://sahay-n69u.onrender.com";

    // Runs every 14 minutes (14 * 60 * 1000 = 840000 milliseconds)
    @Scheduled(fixedRate = 840000)
    public void keepAlive() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForObject(APP_URL, String.class);
            logger.info("Keep-alive ping sent to: " + APP_URL);
        } catch (Exception e) {
            logger.warn("Keep-alive ping failed: " + e.getMessage());
        }
    }
}