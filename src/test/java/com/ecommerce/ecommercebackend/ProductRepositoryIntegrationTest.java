package com.ecommerce.ecommercebackend;

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.service.ProductDataValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A09 – Integration test dùng H2 in-memory database.
 *
 * <p>{@code @ActiveProfiles("test")} kích hoạt {@code application-test.properties}
 * với H2 thay vì MySQL — đảm bảo test không chạm vào DB dev/prod.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("A09 – ProductRepository Integration Test (H2)")
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDataValidationService validationService;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Lưu và tìm sản phẩm thành công với H2 test DB")
    void shouldSaveAndFindProduct() {
        Product p = Product.builder()
                .name("MacBook Air M2")
                .category("laptop")
                .brand("Apple")
                .price(27990000L)
                .imgUrl("https://example.com/macbook.jpg")
                .status(ProductStatus.ACTIVE)
                .stockQuantity(10)
                .build();

        Product saved = productRepository.save(p);

        assertThat(saved.getId()).isNotNull();
        assertThat(productRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("A04 – Phát hiện sản phẩm thiếu giá")
    void shouldDetectMissingPrice() {
        productRepository.saveAll(List.of(
                Product.builder().name("SP1").brand("A").category("laptop").price(null).imgUrl("x.jpg").status(ProductStatus.ACTIVE).stockQuantity(1).build(),
                Product.builder().name("SP2").brand("B").category("laptop").price(10000L).imgUrl("y.jpg").status(ProductStatus.ACTIVE).stockQuantity(1).build()
        ));

        ProductDataValidationService.ValidationReport report = validationService.validate();

        assertThat(report.missingPriceIds()).hasSize(1);
    }

    @Test
    @DisplayName("A04 – Phát hiện SKU trùng")
    void shouldDetectDuplicateSku() {
        productRepository.saveAll(List.of(
                Product.builder().sku("SKU-001").name("SP1").brand("A").category("laptop").price(10000L).imgUrl("x.jpg").status(ProductStatus.ACTIVE).stockQuantity(1).build(),
                Product.builder().sku("SKU-001").name("SP2").brand("B").category("laptop").price(20000L).imgUrl("y.jpg").status(ProductStatus.ACTIVE).stockQuantity(1).build()
        ));

        ProductDataValidationService.ValidationReport report = validationService.validate();

        assertThat(report.duplicateSkus()).contains("SKU-001");
    }

    @Test
    @DisplayName("Validation report rỗng khi tất cả sản phẩm hợp lệ")
    void shouldReturnCleanReportForValidProducts() {
        productRepository.save(
                Product.builder().sku("SKU-OK").name("Good Laptop").brand("Dell").category("laptop")
                        .price(15000000L).imgUrl("img.jpg").status(ProductStatus.ACTIVE).stockQuantity(5).build()
        );

        ProductDataValidationService.ValidationReport report = validationService.validate();

        assertThat(report.hasErrors()).isFalse();
        assertThat(report.errorCount()).isZero();
    }
}
