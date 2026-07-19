package com.example.dawanow.controller;



import com.example.dawanow.dtos.response.PresenceStatusResponse;
import com.example.dawanow.service.CurrentPharmacistProvider;
import com.example.dawanow.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explicit on-duty toggle + heartbeat for the currently authenticated
 * pharmacist. "me" always resolves from the security context — a
 * pharmacist can only change their own presence, never someone else's.
 */


@RestController
@RequestMapping("/api/v1/pharmacists/me/presence")
@RequiredArgsConstructor

public class PharmacistPresenceController {
    private final PresenceService presenceService;
    private final CurrentPharmacistProvider currentPharmacist;

    @PostMapping("/on-duty")
    public ResponseEntity<PresenceStatusResponse> goOnDuty() {
        Long pharmacistId = currentPharmacist.get().getId();
        return ResponseEntity.ok(presenceService.goOnDuty(pharmacistId));
    }

    @PostMapping("/off-duty")
    public ResponseEntity<PresenceStatusResponse> goOffDuty() {
        Long pharmacistId = currentPharmacist.get().getId();
        return ResponseEntity.ok(presenceService.goOffDuty(pharmacistId));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<PresenceStatusResponse> heartbeat() {
        Long pharmacistId = currentPharmacist.get().getId();
        return ResponseEntity.ok(presenceService.heartbeat(pharmacistId));
    }

}
