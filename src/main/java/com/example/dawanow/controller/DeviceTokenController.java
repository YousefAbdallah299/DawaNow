package com.example.dawanow.controller;

import com.example.dawanow.dtos.request.RegisterDeviceTokenRequest;
import com.example.dawanow.service.CurrentPharmacistProvider;
import com.example.dawanow.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pharmacists/me/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;
    private final CurrentPharmacistProvider currentPharmacist;

    @PostMapping("/token")
    public ResponseEntity<Void> registerToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        Long pharmacistId = currentPharmacist.get().getId();
        deviceTokenService.register(pharmacistId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> unregisterToken(@RequestParam String fcmToken) {
        Long pharmacistId = currentPharmacist.get().getId();
        deviceTokenService.unregister(pharmacistId, fcmToken);
        return ResponseEntity.noContent().build();
    }
}
