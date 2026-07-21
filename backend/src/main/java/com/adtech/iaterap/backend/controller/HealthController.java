package com.adtech.iaterap.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "IATERAP Backend is Running!";
    }

    @GetMapping("/health")
    public String health() {
        return "Backend is Healthy!";
    }
}