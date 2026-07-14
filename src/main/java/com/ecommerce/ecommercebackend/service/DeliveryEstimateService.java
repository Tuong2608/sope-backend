package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.DeliveryEstimateItemRequest;
import com.ecommerce.ecommercebackend.dto.request.DeliveryEstimateRequest;
import com.ecommerce.ecommercebackend.dto.response.DeliveryEstimateResponse;
import com.ecommerce.ecommercebackend.entity.Product;
import com.ecommerce.ecommercebackend.entity.ShippingMethod;
import com.ecommerce.ecommercebackend.entity.ShippingRate;
import com.ecommerce.ecommercebackend.entity.ShippingZone;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.repository.HolidayRepository;
import com.ecommerce.ecommercebackend.repository.ProductRepository;
import com.ecommerce.ecommercebackend.repository.ShippingMethodRepository;
import com.ecommerce.ecommercebackend.repository.ShippingRateRepository;
import com.ecommerce.ecommercebackend.repository.ShippingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Computes the shipping fee and expected delivery window for an order
 * (task C04): zone (by province) + method → base fee/days; late orders (past
 * the daily cutoff) and out-of-stock items push the window back further;
 * the final dates skip configured {@link com.ecommerce.ecommercebackend.entity.Holiday}s.
 */
@Service
@RequiredArgsConstructor
public class DeliveryEstimateService {

    private static final String DEFAULT_METHOD_CODE = "STANDARD";

    private final ShippingZoneRepository zoneRepository;
    private final ShippingMethodRepository methodRepository;
    private final ShippingRateRepository rateRepository;
    private final HolidayRepository holidayRepository;
    private final ProductRepository productRepository;

    /** Hour of day (Vietnam local time) after which an order is processed the next day. */
    @Value("${app.shipping.cutoff-hour:18}")
    private int cutoffHour;

    /** Extra days added to the window when an item doesn't have enough stock. */
    @Value("${app.shipping.restock-delay-days:3}")
    private int restockDelayDays;

    /** Estimates using the current time as the order time. */
    @Transactional(readOnly = true)
    public DeliveryEstimateResponse estimate(DeliveryEstimateRequest request) {
        return estimate(request, LocalDateTime.now());
    }

    /**
     * Estimates using an explicit order time — the overload unit tests use to
     * exercise cutoff-hour edge cases deterministically.
     *
     * @throws BadRequestException if the province matches no zone, or the zone
     *                             has no rate configured for the requested method
     */
    @Transactional(readOnly = true)
    public DeliveryEstimateResponse estimate(DeliveryEstimateRequest request, LocalDateTime orderTime) {
        ShippingZone zone = findZone(request.getProvince());

        String methodCode = StringUtils.hasText(request.getMethodCode())
                ? request.getMethodCode().trim()
                : DEFAULT_METHOD_CODE;
        ShippingMethod method = methodRepository.findByCodeIgnoreCase(methodCode)
                .orElseThrow(() -> new BadRequestException(
                        "Phương thức giao hàng không hợp lệ: " + methodCode));

        ShippingRate rate = rateRepository.findByZoneIdAndMethodIdAndActiveTrue(zone.getId(), method.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Khu vực '" + zone.getName() + "' chưa hỗ trợ phương thức " + method.getName()));

        LocalDate baseDate = isPastCutoff(orderTime)
                ? orderTime.toLocalDate().plusDays(1)
                : orderTime.toLocalDate();

        boolean needsRestock = hasOutOfStockItem(request.getItems());
        int minDays = rate.getMinDays() + (needsRestock ? restockDelayDays : 0);
        int maxDays = rate.getMaxDays() + (needsRestock ? restockDelayDays : 0);

        LocalDate estimatedMinDate = addDaysSkippingHolidays(baseDate, minDays);
        LocalDate estimatedMaxDate = addDaysSkippingHolidays(baseDate, maxDays);

        return DeliveryEstimateResponse.builder()
                .zoneName(zone.getName())
                .methodCode(method.getCode())
                .methodName(method.getName())
                .fee(rate.getFee())
                .estimatedMinDate(estimatedMinDate)
                .estimatedMaxDate(estimatedMaxDate)
                .note(needsRestock
                        ? "Một số sản phẩm cần thêm thời gian chuẩn bị do tạm hết hàng."
                        : null)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private ShippingZone findZone(String province) {
        String normalized = province.trim();
        return zoneRepository.findByActiveTrueOrderByPriorityAsc().stream()
                .filter(zone -> zone.getProvinces().stream()
                        .anyMatch(p -> p.equalsIgnoreCase(normalized)))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Chưa hỗ trợ giao hàng tới tỉnh/thành: " + province));
    }

    private boolean isPastCutoff(LocalDateTime orderTime) {
        return orderTime.getHour() >= cutoffHour;
    }

    private boolean hasOutOfStockItem(List<DeliveryEstimateItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        for (DeliveryEstimateItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null || product.getAvailableQuantity() < item.getQuantity()) {
                return true;
            }
        }
        return false;
    }

    /** Adds {@code days} calendar days, then rolls forward past any configured holiday. */
    private LocalDate addDaysSkippingHolidays(LocalDate start, int days) {
        LocalDate date = start.plusDays(days);
        while (holidayRepository.existsByDate(date)) {
            date = date.plusDays(1);
        }
        return date;
    }
}
