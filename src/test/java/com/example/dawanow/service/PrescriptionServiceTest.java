package com.example.dawanow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.dawanow.ai.PrescriptionAiClient;
import com.example.dawanow.dtos.ai.ExtractedMedicine;
import com.example.dawanow.dtos.ai.ExtractedPrescription;
import com.example.dawanow.dtos.response.PrescriptionMatchStatus;
import com.example.dawanow.dtos.response.PrescriptionMedicineResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PrescriptionServiceTest {

    private PrescriptionAiClient aiClient;
    private PrescriptionProductMatchingService matchingService;
    private PrescriptionService service;

    @BeforeEach
    void setUp() {
        aiClient = mock(PrescriptionAiClient.class);
        matchingService = mock(PrescriptionProductMatchingService.class);
        service = new PrescriptionService(aiClient, matchingService);
    }

    @Test
    void analyzesValidJpegAndMatchesEveryMedicine() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        var file = new MockMultipartFile("image", "rx.jpg", "image/jpeg", jpeg);
        var medicine = new ExtractedMedicine("Abilify", "Abilify", "15 mg", "tablets", 0.9);
        var matched = new PrescriptionMedicineResponse(
                UUID.randomUUID(), "Abilify", "Abilify", "15 mg", "tablets",
                PrescriptionMatchStatus.MATCHED, 0.9, List.of()
        );
        when(aiClient.analyze(jpeg, "image/jpeg", "en", "request-key"))
                .thenReturn(new ExtractedPrescription(List.of(medicine)));
        when(matchingService.match(medicine, "en")).thenReturn(matched);

        var response = service.analyze(file, " EN ", "request-key");

        assertThat(response.medicines()).containsExactly(matched);
        verify(aiClient).analyze(jpeg, "image/jpeg", "en", "request-key");
        verify(matchingService).match(medicine, "en");
    }

    @Test
    void rejectsEmptyUnsupportedOrSpoofedImages() {
        var empty = new MockMultipartFile("image", "rx.jpg", "image/jpeg", new byte[0]);
        var text = new MockMultipartFile("image", "rx.txt", "text/plain", "hello".getBytes());
        var spoofed = new MockMultipartFile("image", "rx.jpg", "image/jpeg", "hello".getBytes());

        assertThatThrownBy(() -> service.analyze(empty, "en", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prescription image is required");
        assertThatThrownBy(() -> service.analyze(text, "en", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prescription image must be JPEG or PNG");
        assertThatThrownBy(() -> service.analyze(spoofed, "en", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prescription image content is invalid");
    }

    @Test
    void rejectsUnsupportedLanguage() {
        var file = new MockMultipartFile(
                "image", "rx.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        assertThatThrownBy(() -> service.analyze(file, "fr", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Language must be either en or ar");
    }
}
