package com.ecommerce.ecommercebackend.seeder;

import com.ecommerce.ecommercebackend.entity.Holiday;
import com.ecommerce.ecommercebackend.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * C03 – Seed sẵn vài ngày nghỉ lễ Việt Nam 2026 khi khởi động, để C04 có dữ liệu
 * ngay khi tính ngày giao dự kiến. Admin có thể thêm/sửa trực tiếp qua bảng
 * {@code holidays} (chưa có API quản trị riêng — nằm ngoài phạm vi C03).
 */
@Slf4j
@Component
@Order(31)
@Profile("!test")
@RequiredArgsConstructor
public class HolidayDataSeeder implements ApplicationRunner {

    private final HolidayRepository holidayRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (holidayRepository.count() > 0) {
            return;
        }

        List<Holiday> holidays = List.of(
                Holiday.builder().date(LocalDate.of(2026, 1, 1)).description("Tết Dương lịch").build(),
                Holiday.builder().date(LocalDate.of(2026, 2, 16)).description("Tết Nguyên đán").build(),
                Holiday.builder().date(LocalDate.of(2026, 2, 17)).description("Tết Nguyên đán").build(),
                Holiday.builder().date(LocalDate.of(2026, 2, 18)).description("Tết Nguyên đán").build(),
                Holiday.builder().date(LocalDate.of(2026, 4, 26)).description("Giỗ Tổ Hùng Vương").build(),
                Holiday.builder().date(LocalDate.of(2026, 4, 30)).description("Ngày Giải phóng miền Nam").build(),
                Holiday.builder().date(LocalDate.of(2026, 5, 1)).description("Ngày Quốc tế Lao động").build(),
                Holiday.builder().date(LocalDate.of(2026, 9, 2)).description("Ngày Quốc khánh").build()
        );

        holidayRepository.saveAll(holidays);
        log.info("Seeded {} default Vietnamese holidays for 2026", holidays.size());
    }
}
