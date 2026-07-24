package com.example.dawanow.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dawanow.ai.DemoPrescriptionAiClient;
import com.example.dawanow.ai.ItiPrescriptionAiClient;
import com.example.dawanow.ai.PrescriptionAiClient;
import com.example.dawanow.ai.UnavailablePrescriptionAiClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PrescriptionAiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PrescriptionAiConfig.class);

    @Test
    void selectsItiWhenConfigured() {
        contextRunner.withPropertyValues(
                        "dawanow.ai.prescription.provider=iti",
                        "dawanow.ai.prescription.iti.endpoint-url=http://iti.test/multimodal-chat"
                )
                .run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                        .isInstanceOf(ItiPrescriptionAiClient.class));
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
    void selectsItiWithoutServerSideKeyConfiguration() {
        contextRunner.withPropertyValues("dawanow.ai.prescription.provider=iti")
                .run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                        .isInstanceOf(ItiPrescriptionAiClient.class));
    }

    @Test
    void defaultsToItiWhenConfigurationIsMissing() {
        contextRunner.run(context -> assertThat(context.getBean(PrescriptionAiClient.class))
                .isInstanceOf(ItiPrescriptionAiClient.class));
    }

    @Test
    void defaultsToConfiguredQwenModelAndItiEndpoint() {
        PrescriptionAiProperties properties = new PrescriptionAiProperties();
        assertThat(properties.getIti().getModel()).isEqualTo("qwen.qwen3-vl-235b-a22b");
        assertThat(properties.getIti().getEndpointUrl())
                .isEqualTo("http://apiaccess.iti.net.eg/api/v1/student/multimodal-chat");
    }

    @Test
    void defaultsToSixtySecondItiReadTimeout() {
        assertThat(new PrescriptionAiProperties().getIti().getReadTimeout())
                .isEqualTo(Duration.ofSeconds(60));
    }
}
