package com.example.dawanow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.dawanow.ai.PrescriptionAiClient;
import com.example.dawanow.dtos.ai.ExtractedMedicine;
import com.example.dawanow.dtos.response.ProductResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MedicineImageSearchServiceTest {

    private PrescriptionAiClient aiClient;
    private PrescriptionProductMatchingService matchingService;
    private MedicineImageSearchService service;

    @BeforeEach
    void setUp() {
        aiClient = mock(PrescriptionAiClient.class);
        matchingService = mock(PrescriptionProductMatchingService.class);
        service = new MedicineImageSearchService(aiClient, matchingService, new MedicineImageValidator());
    }

    @Test
    void recognizesMedicinePackageAndReturnsCatalogMatches() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        var image = new MockMultipartFile("image", "panadol.jpg", "image/jpeg", jpeg);
        var extracted = new ExtractedMedicine("Panadol", "Panadol", null, null, 1.0);
        var products = List.of(product(1L, "PANADOL"));
        when(aiClient.analyzeMedicineImage(jpeg, "image/jpeg", "en", "request-key"))
                .thenReturn(Optional.of(extracted));
        when(matchingService.findTopProductsByMedicineName("Panadol", "en")).thenReturn(products);

        assertThat(service.analyzeImage(image, " EN ", "request-key")).containsExactlyElementsOf(products);
        verify(aiClient).analyzeMedicineImage(jpeg, "image/jpeg", "en", "request-key");
        verify(matchingService).findTopProductsByMedicineName("Panadol", "en");
    }

    @Test
    void returnsEmptyListWhenAiDoesNotReadAMedicineName() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        var image = new MockMultipartFile("image", "unknown.jpg", "image/jpeg", jpeg);
        when(aiClient.analyzeMedicineImage(jpeg, "image/jpeg", "en", "request-key"))
                .thenReturn(Optional.empty());

        assertThat(service.analyzeImage(image, "en", "request-key")).isEmpty();
    }

    @Test
    void rejectsInvalidMedicineImages() {
        var image = new MockMultipartFile("image", "item.jpg", "image/jpeg", "not-an-image".getBytes());

        assertThatThrownBy(() -> service.analyzeImage(image, "en", "request-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Medicine image content is invalid");
    }

    private ProductResponse product(Long id, String name) {
        return new ProductResponse(
                id, name, name, null, null, null, null, null, null, null,
                null, null, null, null, null
        );
    }
}
