package com.cansu.springboot.veriislemebackend.controller;

import com.cansu.springboot.veriislemebackend.service.SupersetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/superset")
public class SupersetController {
    private final SupersetService supersetService;

    public SupersetController(SupersetService supersetService) {
        this.supersetService = supersetService;
    }

    @GetMapping("/guest-token")
    public ResponseEntity<String> getGuestToken(@RequestParam("dashboardId") String dashboardId) {
        try {
            String token = supersetService.getGuestToken(dashboardId); //Servise gider, Superset ile konuşur ve Token üretir
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Superset token alinamadi: " + e.getMessage());
        }
    }
}
