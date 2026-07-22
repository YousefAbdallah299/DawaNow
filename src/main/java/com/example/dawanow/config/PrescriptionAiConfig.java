package com.example.dawanow.config;

import com.example.dawanow.ai.DemoPrescriptionAiClient;
import com.example.dawanow.ai.GeminiPrescriptionAiClient;
import com.example.dawanow.ai.PrescriptionAiClient;
import com.example.dawanow.ai.UnavailablePrescriptionAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PrescriptionAiProperties.class)
public class PrescriptionAiConfig {

    @Bean
    PrescriptionAiClient prescriptionAiClient(
            PrescriptionAiProperties properties,
            Environment environment
    ) {
        return switch (properties.getProvider()) {
            case DEMO -> new DemoPrescriptionAiClient();
            case DISABLED -> new UnavailablePrescriptionAiClient();
            case GEMINI -> geminiClient(properties.getGemini(), environment);
        };
    }

    private PrescriptionAiClient geminiClient(
            PrescriptionAiProperties.Gemini properties,
            Environment environment
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(stripTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
        return new GeminiPrescriptionAiClient(
                restClient,
                new ObjectMapper(),
                properties.getModel(),
                stripTrailingSlash(properties.getBaseUrl()),
                isGeminiModelOverridden(environment)
        );
    }

    private boolean isGeminiModelOverridden(Environment environment) {
        String value = environment.getProperty("GEMINI_MODEL");
        return value != null && !value.isBlank();
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
