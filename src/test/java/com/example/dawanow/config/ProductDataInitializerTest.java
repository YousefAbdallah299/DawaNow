package com.example.dawanow.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dawanow.entity.Category;
import com.example.dawanow.entity.Product;
import com.example.dawanow.repo.CategoryRepository;
import com.example.dawanow.repo.ProductRepository;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "dawanow.data.products.import-enabled=true")
class ProductDataInitializerTest {

    private static final Set<String> VALID_ROUTES = Set.of(
            "EAR",
            "EFF",
            "EYE",
            "INJECTION",
            "MOUTH",
            "ORAL.LIQUID",
            "ORAL.SOLID",
            "RECTAL",
            "SPRAY",
            "TOPICAL"
    );
    private static final Pattern NON_MEDICINE_CATEGORY = Pattern.compile(
            "MILK PRODUCTS?|INFANT FORMULA|BABY FOOD|COSMETIC|SKIN CARE|HAIR CARE|"
                    + "BODY CARE|PERSONAL CARE|SUPPLEMENT|NUTRITION|DRINKS|SWEETENER|PURIFIED WATER"
    );

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @Transactional
    void importsProductDataset() {
        List<Product> products = productRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        assertThat(products).hasSize(798);
        assertThat(categories).hasSize(226);
        assertThat(categories)
                .extracting(Category::getName)
                .doesNotHaveDuplicates()
                .allMatch(name -> !name.isBlank() && name.equals(name.toUpperCase()))
                .noneMatch(name -> NON_MEDICINE_CATEGORY.matcher(name).find());
        assertThat(products)
                .extracting(Product::getName, Product::getPrice)
                .doesNotHaveDuplicates();
        assertThat(products.stream()
                .map(product -> product.getCategory().getId())
                .collect(Collectors.toSet()))
                .hasSize(categories.size());
        assertThat(products).allSatisfy(product -> {
            assertThat(product.getName()).isNotBlank();
            assertThat(product.getArabicName()).isNotBlank();
            assertThat(product.getScientificName()).isNotBlank();
            assertThat(product.getPrice()).isPositive();
            assertThat(product.getImageUrl()).startsWith("https://");
            assertThat(product.getCategory()).isNotNull();
            assertThat(product.getCategory().getId()).isNotNull();
            assertThat(product.getCompany()).isNotBlank();
            assertThat(product.getRoute()).isIn(VALID_ROUTES);
        });
    }
}
