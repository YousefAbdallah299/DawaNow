package com.example.dawanow.service;

import com.example.dawanow.ai.PrescriptionAiClient;
import com.example.dawanow.dtos.ai.ExtractedPrescription;
import com.example.dawanow.dtos.response.PrescriptionAnalysisResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final PrescriptionAiClient prescriptionAiClient;
    private final PrescriptionProductMatchingService matchingService;

    public PrescriptionAnalysisResponse analyze(MultipartFile image, String lang, String geminiApiKey) {
        String language = normalizeLanguage(lang);
        validateImage(image);

        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read prescription image", exception);
        }
        validateSignature(bytes, image.getContentType());

        ExtractedPrescription extracted = prescriptionAiClient.analyze(
                bytes,
                image.getContentType(),
                language,
                geminiApiKey
        );
        if (extracted == null || extracted.medicines() == null) {
            return new PrescriptionAnalysisResponse(List.of());
        }

        return new PrescriptionAnalysisResponse(
                extracted.medicines().stream()
                        .map(medicine -> matchingService.match(medicine, language))
                        .toList()
        );
    }

    private String normalizeLanguage(String lang) {
        String language = StringUtils.hasText(lang) ? lang.trim().toLowerCase() : "en";
        if (!Set.of("en", "ar").contains(language)) {
            throw new IllegalArgumentException("Language must be either en or ar");
        }
        return language;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Prescription image is required");
        }
        if (!SUPPORTED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new IllegalArgumentException("Prescription image must be JPEG or PNG");
        }
    }

    private void validateSignature(byte[] bytes, String contentType) {
        boolean validJpeg = bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
        boolean validPng = bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
        if (("image/jpeg".equals(contentType) && !validJpeg)
                || ("image/png".equals(contentType) && !validPng)) {
            throw new IllegalArgumentException("Prescription image content is invalid");
        }
    }
}
