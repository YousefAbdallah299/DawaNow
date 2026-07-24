package com.example.dawanow.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.dawanow.exception.PrescriptionAiUnavailableException;
import org.junit.jupiter.api.Test;

class DemoPrescriptionAiClientTest {

    @Test
    void returnsEnglishDemoMedicines() {
        var result = new DemoPrescriptionAiClient().analyze(new byte[]{1}, "image/jpeg", "en", null);

        assertThat(result.medicines()).hasSize(2);
        assertThat(result.medicines().getFirst().name()).isEqualTo("Abilify");
        assertThat(result.medicines().getFirst().strength()).isEqualTo("15 mg");
    }

    @Test
    void returnsArabicDemoMedicines() {
        var result = new DemoPrescriptionAiClient().analyze(new byte[]{1}, "image/jpeg", "ar", null);

        assertThat(result.medicines()).hasSize(2);
        assertThat(result.medicines().getFirst().name()).isEqualTo("أبيليفي");
    }

    @Test
    void unavailableClientFailsClearly() {
        assertThatThrownBy(() -> new UnavailablePrescriptionAiClient()
                .analyze(new byte[]{1}, "image/jpeg", "en", null))
                .isInstanceOf(PrescriptionAiUnavailableException.class)
                .hasMessage("Prescription AI is not configured");
    }
}
