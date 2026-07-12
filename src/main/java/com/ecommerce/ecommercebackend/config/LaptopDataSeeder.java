package com.ecommerce.ecommercebackend.config;

import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ProductStatus;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A03 – ApplicationRunner tự động seed dữ liệu laptop mẫu khi khởi động.
 *
 * <p>Chỉ chạy khi:
 * <ul>
 *   <li>Profile không phải {@code test} (A09 dùng DB test riêng)</li>
 *   <li>Chưa có sản phẩm nào có category="laptop" trong DB</li>
 * </ul>
 * </p>
 *
 * <p>Để import dữ liệu laptop từ file crawl thật, dùng admin API:
 * {@code POST /api/admin/products/import/laptop}</p>
 */
@Slf4j
@Component
@Order(20)          // Chạy sau AdminSeeder (Order 10)
@Profile("!test")   // Không chạy khi test
@RequiredArgsConstructor
public class LaptopDataSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(ApplicationArguments args) {
        long existingLaptops = productRepository.findAll().stream()
                .filter(p -> "laptop".equals(p.getCategory()))
                .count();

        if (existingLaptops > 0) {
            log.info("[A03] Đã có {} laptop trong DB — bỏ qua seeding", existingLaptops);
            return;
        }

        List<Product> samples = buildSampleLaptops();
        productRepository.saveAll(samples);
        log.info("[A03] Đã seed {} laptop mẫu vào DB", samples.size());
    }

    /**
     * 10 laptop mẫu đại diện cho các hãng phổ biến.
     * Dùng để dev/demo khi chưa có file crawl thật.
     */
    private List<Product> buildSampleLaptops() {
        return List.of(
                build("LAPTOP-001", "MacBook Air M2 2024",            "Apple",  27990000L),
                build("LAPTOP-002", "MacBook Pro M3 14 inch",          "Apple",  49990000L),
                build("LAPTOP-003", "Dell XPS 13 Plus",                "Dell",   35990000L),
                build("LAPTOP-004", "Dell Inspiron 15 3530",           "Dell",   16990000L),
                build("LAPTOP-005", "HP Spectre x360 14",              "HP",     42990000L),
                build("LAPTOP-006", "HP Pavilion 15 EG3000",           "HP",     18990000L),
                build("LAPTOP-007", "Lenovo ThinkPad X1 Carbon Gen12", "Lenovo", 45990000L),
                build("LAPTOP-008", "Lenovo IdeaPad Slim 5 16IAH8",    "Lenovo", 19990000L),
                build("LAPTOP-009", "ASUS Zenbook 14 OLED",            "ASUS",   25990000L),
                build("LAPTOP-010", "ASUS TUF Gaming F15 2024",        "ASUS",   21990000L)
        );
    }

    private Product build(String sku, String name, String brand, Long price) {
        return Product.builder()
                .sku(sku)
                .name(name)
                .brand(brand)
                .category("laptop")
                .price(price)
                .oldPrice((long) (price * 1.1))
                .imgUrl("/images/placeholder-laptop.png")
                .status(ProductStatus.ACTIVE)
                .stockQuantity(50)
                .minStockLevel(5)
                .build();
    }
}
