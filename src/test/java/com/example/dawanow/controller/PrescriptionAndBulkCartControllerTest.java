package com.example.dawanow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dawanow.dtos.response.PrescriptionAnalysisResponse;
import com.example.dawanow.exception.PrescriptionAiUnavailableException;
import com.example.dawanow.service.CartService;
import com.example.dawanow.service.PrescriptionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(properties = "dawanow.data.products.import-enabled=false")
@AutoConfigureMockMvc
class PrescriptionAndBulkCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrescriptionService prescriptionService;

    @MockitoBean
    private CartService cartService;

    @Test
    void prescriptionAnalysisRequiresAuthentication() throws Exception {
        MockMultipartFile image = jpeg();

        mockMvc.perform(multipart("/api/v1/prescriptions/analyze").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsPrescriptionAnalysisForAuthenticatedUser() throws Exception {
        MockMultipartFile image = jpeg();
        when(prescriptionService.analyze(any(), eq("en"), eq("valid-key")))
                .thenReturn(new PrescriptionAnalysisResponse(List.of()));

        mockMvc.perform(multipart("/api/v1/prescriptions/analyze")
                        .file(image)
                        .param("lang", "en")
                        .header("X-Gemini-Api-Key", "valid-key")
                        .with(user("patient")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.medicines").isArray());
    }

    @Test
    void mapsInvalidImageToBadRequest() throws Exception {
        MockMultipartFile image = jpeg();
        when(prescriptionService.analyze(any(), eq("en"), isNull()))
                .thenThrow(new IllegalArgumentException("Prescription image content is invalid"));

        mockMvc.perform(multipart("/api/v1/prescriptions/analyze")
                        .file(image)
                        .with(user("patient")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prescription image content is invalid"));
    }

    @Test
    void mapsMissingGeminiHeaderToServiceUnavailable() throws Exception {
        MockMultipartFile image = jpeg();
        when(prescriptionService.analyze(any(), eq("en"), isNull()))
                .thenThrow(new PrescriptionAiUnavailableException("Prescription AI is not configured"));

        mockMvc.perform(multipart("/api/v1/prescriptions/analyze")
                        .file(image)
                        .with(user("patient")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Prescription AI is not configured"));
    }

    @Test
    void mapsInvalidGeminiHeaderToServiceUnavailable() throws Exception {
        MockMultipartFile image = jpeg();
        when(prescriptionService.analyze(any(), eq("en"), eq("invalid-key")))
                .thenThrow(new PrescriptionAiUnavailableException("Prescription AI provider is unavailable"));

        mockMvc.perform(multipart("/api/v1/prescriptions/analyze")
                        .file(image)
                        .header("X-Gemini-Api-Key", "invalid-key")
                        .with(user("patient")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Prescription AI provider is unavailable"));
    }

    @Test
    void documentsOptionalGeminiApiKeyHeader() throws Exception {
        String parameterPath = "$.paths['/api/v1/prescriptions/analyze'].post.parameters"
                + "[?(@.name == 'X-Gemini-Api-Key')]";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(parameterPath).isNotEmpty())
                .andExpect(jsonPath(parameterPath + ".in").value(hasItem("header")))
                .andExpect(jsonPath(parameterPath + ".required").value(hasItem(false)));
    }

    @Test
    void rejectsEmptyBulkCartRequest() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items/bulk")
                        .with(user("patient"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsInvalidBulkCartQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items/bulk")
                        .with(user("patient"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private MockMultipartFile jpeg() {
        return new MockMultipartFile(
                "image",
                "prescription.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );
    }
}
