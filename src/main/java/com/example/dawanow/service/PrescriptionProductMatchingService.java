package com.example.dawanow.service;

import com.example.dawanow.dtos.ai.ExtractedMedicine;
import com.example.dawanow.dtos.response.PrescriptionCandidateResponse;
import com.example.dawanow.dtos.response.PrescriptionMatchStatus;
import com.example.dawanow.dtos.response.PrescriptionMedicineResponse;
import com.example.dawanow.entity.Product;
import com.example.dawanow.entity.ProductTranslation;
import com.example.dawanow.repo.ProductRepository;
import com.example.dawanow.repo.ProductTranslationRepository;
import com.example.dawanow.util.MedicineTextNormalizer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrescriptionProductMatchingService {

    private static final double MIN_READABLE_CONFIDENCE = 0.5;
    private static final int MAX_CANDIDATES = 3;

    private final ProductRepository productRepository;
    private final ProductTranslationRepository productTranslationRepository;
    private final MedicineTextNormalizer normalizer;

    @Transactional(readOnly = true)
    public PrescriptionMedicineResponse match(ExtractedMedicine medicine, String language) {
        if (medicine == null
                || medicine.confidence() < MIN_READABLE_CONFIDENCE
                || normalizer.normalizeName(medicine.name()).isEmpty()) {
            return response(medicine, PrescriptionMatchStatus.UNREADABLE, List.of());
        }

        List<CatalogProduct> catalog = loadCatalog(language);
        String extractedName = normalizer.normalizeName(medicine.name());
        List<CatalogProduct> exactNameMatches = catalog.stream()
                .filter(product -> extractedName.equals(normalizer.normalizeName(product.productName())))
                .toList();

        if (exactNameMatches.isEmpty()) {
            List<CatalogProduct> suggestions = catalog.stream()
                    .filter(product -> {
                        String productName = normalizer.normalizeName(product.productName());
                        return productName.contains(extractedName) || extractedName.contains(productName);
                    })
                    .sorted(candidateComparator(medicine))
                    .limit(MAX_CANDIDATES)
                    .toList();
            return response(medicine, PrescriptionMatchStatus.NOT_FOUND, suggestions);
        }

        List<CatalogProduct> compatible = new ArrayList<>(exactNameMatches);
        String extractedStrength = normalizer.normalizeStrength(medicine.strength());
        if (!extractedStrength.isEmpty()) {
            compatible = compatible.stream()
                    .filter(product -> extractedStrength.equals(normalizer.normalizeStrength(product.strength())))
                    .toList();
            if (compatible.isEmpty()) {
                return response(
                        medicine,
                        PrescriptionMatchStatus.NEEDS_REVIEW,
                        exactNameMatches.stream().sorted(candidateComparator(medicine)).limit(MAX_CANDIDATES).toList()
                );
            }
        }

        String extractedForm = normalizer.normalizeForm(medicine.form());
        if (!extractedForm.isEmpty()) {
            List<CatalogProduct> formMatches = compatible.stream()
                    .filter(product -> extractedForm.equals(normalizer.normalizeForm(product.form())))
                    .toList();
            if (formMatches.isEmpty()) {
                return response(
                        medicine,
                        PrescriptionMatchStatus.NEEDS_REVIEW,
                        compatible.stream().sorted(candidateComparator(medicine)).limit(MAX_CANDIDATES).toList()
                );
            }
            compatible = formMatches;
        }

        PrescriptionMatchStatus status = compatible.size() == 1
                ? PrescriptionMatchStatus.MATCHED
                : PrescriptionMatchStatus.NEEDS_REVIEW;
        return response(
                medicine,
                status,
                compatible.stream().sorted(candidateComparator(medicine)).limit(MAX_CANDIDATES).toList()
        );
    }

    private List<CatalogProduct> loadCatalog(String language) {
        if ("ar".equals(language)) {
            return productTranslationRepository.findAllByLang("ar").stream()
                    .map(this::toCatalogProduct)
                    .toList();
        }
        return productRepository.findAll().stream().map(this::toCatalogProduct).toList();
    }

    private Comparator<CatalogProduct> candidateComparator(ExtractedMedicine medicine) {
        String strength = normalizer.normalizeStrength(medicine.strength());
        String form = normalizer.normalizeForm(medicine.form());
        return Comparator
                .comparing((CatalogProduct product) -> !strength.isEmpty()
                        && strength.equals(normalizer.normalizeStrength(product.strength())))
                .reversed()
                .thenComparing(
                        (CatalogProduct product) -> !form.isEmpty()
                                && form.equals(normalizer.normalizeForm(product.form())),
                        Comparator.reverseOrder()
                )
                .thenComparing(CatalogProduct::id);
    }

    private PrescriptionMedicineResponse response(
            ExtractedMedicine medicine,
            PrescriptionMatchStatus status,
            List<CatalogProduct> candidates
    ) {
        return new PrescriptionMedicineResponse(
                UUID.randomUUID(),
                medicine == null ? null : medicine.rawText(),
                medicine == null ? null : medicine.name(),
                medicine == null ? null : medicine.strength(),
                medicine == null ? null : medicine.form(),
                status,
                medicine == null ? 0 : medicine.confidence(),
                candidates.stream().map(this::toResponse).toList()
        );
    }

    private PrescriptionCandidateResponse toResponse(CatalogProduct product) {
        return new PrescriptionCandidateResponse(
                product.id(),
                product.name(),
                product.strength(),
                product.form(),
                product.price(),
                product.imageUrl()
        );
    }

    private CatalogProduct toCatalogProduct(Product product) {
        return new CatalogProduct(
                product.getId(), product.getName(), product.getProductName(), product.getStrength(),
                product.getForm(), product.getPrice(), product.getImageUrl()
        );
    }

    private CatalogProduct toCatalogProduct(ProductTranslation translation) {
        Product product = translation.getProduct();
        return new CatalogProduct(
                product.getId(), translation.getName(), translation.getProductName(), translation.getStrength(),
                translation.getForm(), product.getPrice(), product.getImageUrl()
        );
    }

    private record CatalogProduct(
            Long id,
            String name,
            String productName,
            String strength,
            String form,
            BigDecimal price,
            String imageUrl
    ) {
    }
}
