package com.ecommerce.ecommercebackend.seeder;

import com.ecommerce.ecommercebackend.entity.ShippingMethod;
import com.ecommerce.ecommercebackend.entity.ShippingRate;
import com.ecommerce.ecommercebackend.entity.ShippingZone;
import com.ecommerce.ecommercebackend.repository.ShippingMethodRepository;
import com.ecommerce.ecommercebackend.repository.ShippingRateRepository;
import com.ecommerce.ecommercebackend.repository.ShippingZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * C02 – Seed sẵn dữ liệu mẫu cho khu vực/phương thức/phí giao hàng khi khởi động,
 * để C04/C05 (tính và xem trước ngày giao) có dữ liệu ngay mà không cần API quản trị riêng.
 *
 * <p>Chỉ chạy khi profile không phải {@code test} và bảng {@code shipping_zones}
 * đang trống (không seed trùng, không ghi đè dữ liệu admin đã tuỳ chỉnh).</p>
 */
@Slf4j
@Component
@Order(30)
@Profile("!test")
@RequiredArgsConstructor
public class ShippingDataSeeder implements ApplicationRunner {

    private final ShippingZoneRepository zoneRepository;
    private final ShippingMethodRepository methodRepository;
    private final ShippingRateRepository rateRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (zoneRepository.count() > 0) {
            return;
        }

        ShippingMethod standard = methodRepository.save(
                ShippingMethod.builder().code("STANDARD").name("Giao hàng tiêu chuẩn").build());
        ShippingMethod express = methodRepository.save(
                ShippingMethod.builder().code("EXPRESS").name("Giao hàng nhanh").build());

        ShippingZone innerHanoi = zoneRepository.save(ShippingZone.builder()
                .name("Nội thành Hà Nội")
                .provinces(Set.of("Hà Nội"))
                .priority(1)
                .build());
        ShippingZone innerHcm = zoneRepository.save(ShippingZone.builder()
                .name("Nội thành TP. Hồ Chí Minh")
                .provinces(Set.of("TP. Hồ Chí Minh", "Hồ Chí Minh"))
                .priority(1)
                .build());
        ShippingZone north = zoneRepository.save(ShippingZone.builder()
                .name("Miền Bắc")
                .provinces(Set.of("Hà Nội", "Hải Phòng", "Quảng Ninh", "Bắc Ninh", "Thái Nguyên"))
                .priority(10)
                .build());
        ShippingZone central = zoneRepository.save(ShippingZone.builder()
                .name("Miền Trung")
                .provinces(Set.of("Đà Nẵng", "Thừa Thiên Huế", "Khánh Hòa", "Nghệ An", "Quảng Nam"))
                .priority(10)
                .build());
        ShippingZone south = zoneRepository.save(ShippingZone.builder()
                .name("Miền Nam")
                .provinces(Set.of("TP. Hồ Chí Minh", "Hồ Chí Minh", "Bình Dương", "Đồng Nai", "Cần Thơ", "Long An"))
                .priority(10)
                .build());

        saveRate(innerHanoi, standard, 15_000L, 1, 2);
        saveRate(innerHanoi, express, 35_000L, 0, 1);
        saveRate(innerHcm, standard, 15_000L, 1, 2);
        saveRate(innerHcm, express, 35_000L, 0, 1);
        saveRate(north, standard, 25_000L, 2, 4);
        saveRate(north, express, 55_000L, 1, 2);
        saveRate(central, standard, 30_000L, 3, 5);
        saveRate(central, express, 65_000L, 2, 3);
        saveRate(south, standard, 25_000L, 2, 4);
        saveRate(south, express, 55_000L, 1, 2);

        log.info("Seeded default shipping zones/methods/rates (5 zones x 2 methods)");
    }

    private void saveRate(ShippingZone zone, ShippingMethod method, long fee, int minDays, int maxDays) {
        rateRepository.save(ShippingRate.builder()
                .zone(zone)
                .method(method)
                .fee(fee)
                .minDays(minDays)
                .maxDays(maxDays)
                .build());
    }
}
