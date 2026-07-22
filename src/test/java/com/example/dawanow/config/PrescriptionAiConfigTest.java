package com.example.dawanow.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dawanow.ai.DemoPrescriptionAiClient;
import com.example.dawanow.ai.GeminiPrescriptionAiClient;
import com.example.dawanow.ai.PrescriptionAiClient;
import com.example.dawanow.ai.UnavailablePrescriptionAiClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PrescriptionAiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PrescriptionAiConfig.class);

    @Test
    void selectsGeminiWhenConfigured() {
        contextRunner.withPropertyValues(
                        "dawanow.ai.prescription.provider=gemini",
                        "dawanow.ai.prescription.gemini.base-url=https://gemini.test"
                )
                .run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                        .isInstanceOf(GeminiPrescriptionAiClient.class));
    }

    @Test
    void selectsDemoExplicitly() {
        contextRunner.withPropertyValues("dawanow.ai.prescription.provider=demo")
                .run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                        .isInstanceOf(DemoPrescriptionAiClient.class));
    }

    @Test
    void selectsUnavailableWhenDisabled() {
        contextRunner.withPropertyValues("dawanow.ai.prescription.provider=disabled")
                .run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                        .isInstanceOf(UnavailablePrescriptionAiClient.class));
    }

    @Test
    void selectsGeminiWithoutServerSideKeyConfiguration() {
        contextRunner.withPropertyValues("dawanow.ai.prescription.provider=gemini")
                .run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                        .isInstanceOf(GeminiPrescriptionAiClient.class));
    }

    @Test
    void defaultsToGeminiWhenConfigurationIsMissing() {
        contextRunner.run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                .isInstanceOf(GeminiPrescriptionAiClient.class));
    }

    @Test
    void defaultsToOfficialStableGeminiModel() {
        assertThat(new PrescriptionAiProperties().getGemini().getModel())
                .isEqualTo("gemini-3.5-flash");
    }

    @Test
    void defaultsToSixtySecondGeminiReadTimeout() {
        assertThat(new PrescriptionAiProperties().getGemini().getReadTimeout())
                .isEqualTo(Duration.ofSeconds(60));
    }
}
