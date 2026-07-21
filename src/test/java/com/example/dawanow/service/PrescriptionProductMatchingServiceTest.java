package com.example.dawanow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.dawanow.dtos.ai.ExtractedMedicine;
import com.example.dawanow.dtos.response.PrescriptionMatchStatus;
import com.example.dawanow.entity.Product;
import com.example.dawanow.entity.ProductTranslation;
import com.example.dawanow.repo.ProductRepository;
import com.example.dawanow.repo.ProductTranslationRepository;
import com.example.dawanow.util.MedicineTextNormalizer;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrescriptionProductMatchingServiceTest {

    private ProductRepository productRepository;
    private ProductTranslationRepository translationRepository;
    private PrescriptionProductMatchingService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        translationRepository = mock(ProductTranslationRepository.class);
        service = new PrescriptionProductMatchingService(
                productRepository,
                translationRepository,
                new MedicineTextNormalizer()
        );
    }

    @Test
    void matchesExactEnglishNameStrengthAndForm() {
        Product product = product(1L, "ABILIFY 15 MG 10 TABS", "ABILIFY", "15 MG", "TABS");
        when(productRepository.findAll()).thenReturn(List.of(product));

        var result = service.match(
                new ExtractedMedicine("Abilify 15 mg tablets", "Abilify", "15 mg", "tablets", 0.96),
                "en"
        );

        assertThat(result.matchStatus()).isEqualTo(PrescriptionMatchStatus.MATCHED);
        assertThat(result.candidates()).singleElement().satisfies(candidate ->
                assertThat(candidate.productId()).isEqualTo(1L)
        );
    }

    @Test
    void matchesArabicTranslation() {
        Product product = product(1L, "ABILIFY 15 MG 10 TABS", "ABILIFY", "15 MG", "TABS");
        ProductTranslation translation = new ProductTranslation();
        translation.setProduct(product);
        translation.setName("أبيليفي 15 مجم 10 أقراص");
        translation.setProductName("أبيليفي");
        translation.setStrength("15 مجم");
        translation.setForm("أقراص");
        when(translationRepository.findAllByLang("ar")).thenReturn(List.of(translation));

        var result = service.match(
                new ExtractedMedicine("أبيليفي ١٥ مجم", "أبيليفي", "١٥ مجم", "أقراص", 0.94),
                "ar"
        );

        assertThat(result.matchStatus()).isEqualTo(PrescriptionMatchStatus.MATCHED);
        assertThat(result.candidates()).singleElement().satisfies(candidate ->
                assertThat(candidate.name()).isEqualTo("أبيليفي 15 مجم 10 أقراص")
        );
    }

    @Test
    void requiresReviewWhenStrengthDoesNotIdentifyOneVariant() {
        Product low = product(1L, "ABILIFY 5 MG 10 TABS", "ABILIFY", "5 MG", "TABS");
        Product high = product(2L, "ABILIFY 15 MG 10 TABS", "ABILIFY", "15 MG", "TABS");
        when(productRepository.findAll()).thenReturn(List.of(low, high));

        var result = service.match(
                new ExtractedMedicine("Abilify tablets", "Abilify", null, "tablets", 0.9),
                "en"
        );

        assertThat(result.matchStatus()).isEqualTo(PrescriptionMatchStatus.NEEDS_REVIEW);
        assertThat(result.candidates()).hasSize(2);
    }

    @Test
    void marksLowConfidenceOrMissingNameAsUnreadable() {
        var lowConfidence = service.match(
                new ExtractedMedicine("unclear", "Abilify", null, null, 0.2),
                "en"
        );
        var missingName = service.match(
                new ExtractedMedicine("unclear", null, null, null, 0.9),
                "en"
        );

        assertThat(lowConfidence.matchStatus()).isEqualTo(PrescriptionMatchStatus.UNREADABLE);
        assertThat(missingName.matchStatus()).isEqualTo(PrescriptionMatchStatus.UNREADABLE);
    }

    @Test
    void doesNotAutomaticallyMatchApproximateName() {
        Product product = product(1L, "PANADOL EXTRA 24 TABS", "PANADOL EXTRA", null, "TABS");
        when(productRepository.findAll()).thenReturn(List.of(product));

        var result = service.match(
                new ExtractedMedicine("Panadol", "Panadol", null, "tablets", 0.9),
                "en"
        );

        assertThat(result.matchStatus()).isEqualTo(PrescriptionMatchStatus.NOT_FOUND);
        assertThat(result.candidates()).singleElement();
    }

    private Product product(Long id, String name, String productName, String strength, String form) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setProductName(productName);
        product.setStrength(strength);
        product.setForm(form);
        product.setPrice(BigDecimal.TEN);
        product.setImageUrl("https://example.com/product.jpg");
        return product;
    }
}
