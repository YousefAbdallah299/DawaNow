package com.example.dawanow.ai;

import com.example.dawanow.dtos.ai.ExtractedPrescription;

public interface PrescriptionAiClient {

    ExtractedPrescription analyze(byte[] image, String contentType, String language, String apiKey);
}
