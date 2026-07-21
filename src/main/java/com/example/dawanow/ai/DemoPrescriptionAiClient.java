package com.example.dawanow.ai;

import com.example.dawanow.dtos.ai.ExtractedMedicine;
import com.example.dawanow.dtos.ai.ExtractedPrescription;
import java.util.List;

public class DemoPrescriptionAiClient implements PrescriptionAiClient {

    @Override
    public ExtractedPrescription analyze(byte[] image, String contentType, String language, String apiKey) {
        if ("ar".equals(language)) {
            return new ExtractedPrescription(List.of(
                    new ExtractedMedicine("أبيليفي 15 مجم أقراص", "أبيليفي", "15 مجم", "أقراص", 0.96),
                    new ExtractedMedicine("بانادول إيكسترا أقراص", "بانادول إيكسترا", null, "أقراص", 0.88)
            ));
        }

        return new ExtractedPrescription(List.of(
                new ExtractedMedicine("Abilify 15 mg tablets", "Abilify", "15 mg", "tablets", 0.96),
                new ExtractedMedicine("Panadol Extra tablets", "Panadol Extra", null, "tablets", 0.88)
        ));
    }
}
