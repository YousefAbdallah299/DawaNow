package com.example.dawanow.ai;

import com.example.dawanow.dtos.ai.ExtractedPrescription;
import com.example.dawanow.exception.PrescriptionAiUnavailableException;

public class UnavailablePrescriptionAiClient implements PrescriptionAiClient {

    @Override
    public ExtractedPrescription analyze(byte[] image, String contentType, String language, String apiKey) {
        throw new PrescriptionAiUnavailableException("Prescription AI is not configured");
    }
}
