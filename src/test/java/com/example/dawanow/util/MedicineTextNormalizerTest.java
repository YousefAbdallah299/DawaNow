package com.example.dawanow.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MedicineTextNormalizerTest {

    private final MedicineTextNormalizer normalizer = new MedicineTextNormalizer();

    @Test
    void normalizesEnglishAndArabicNames() {
        assertThat(normalizer.normalizeName("  AUGMENTIN® ")).isEqualTo("augmentin");
        assertThat(normalizer.normalizeName("أَبـيليفي")).isEqualTo("ابيليفي");
    }

    @Test
    void normalizesArabicDigitsAndStrengthUnits() {
        assertThat(normalizer.normalizeStrength("١٥ مجم")).isEqualTo("15 mg");
        assertThat(normalizer.normalizeStrength("1 g")).isEqualTo("1000 mg");
    }

    @Test
    void normalizesCommonMedicineForms() {
        assertThat(normalizer.normalizeForm("TABS")).isEqualTo("tablet");
        assertThat(normalizer.normalizeForm("أقراص")).isEqualTo("tablet");
        assertThat(normalizer.normalizeForm("كبسولات")).isEqualTo("capsule");
    }
}
