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

@Service
@RequiredArgsConstructor
public class DeliveryEstimateService {

    private static final String DEFAULT_METHOD_CODE = "STANDARD";

    private final ShippingZoneRepository zoneRepository;
    private final ShippingMethodRepository methodRepository;
    private final ShippingRateRepository rateRepository;
    private final HolidayRepository holidayRepository;
    private final ProductRepository productRepository;

    @Value("${app.shipping.cutoff-hour:18}")
    private int cutoffHour;

    @Value("${app.shipping.restock-delay-days:3}")
    private int restockDelayDays;

    @Transactional(readOnly = true)
    public DeliveryEstimateResponse estimate(DeliveryEstimateRequest request) {
        return estimate(request, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public DeliveryEstimateResponse estimate(DeliveryEstimateRequest request, LocalDateTime orderTime) {
        ShippingZone zone = findZone(request.getProvince());
        String methodCode = StringUtils.hasText(request.getMethodCode())
                ? request.getMethodCode().trim()
                : DEFAULT_METHOD_CODE;

        ShippingMethod method = methodRepository.findByCodeIgnoreCase(methodCode)
                .filter(ShippingMethod::isActive)
                .orElseThrow(() -> new BadRequestException(
                        "Phương thức giao hàng không hợp lệ hoặc đang tạm ngưng: " + methodCode));

        ShippingRate rate = findActiveRate(zone, method);
        return buildEstimate(request, orderTime, zone, method, rate);
    }

    /** Returns all active method/rate combinations available for one province. */
    @Transactional(readOnly = true)
    public List<DeliveryEstimateResponse> estimateOptions(DeliveryEstimateRequest request) {
        ShippingZone zone = findZone(request.getProvince());
        LocalDateTime orderTime = LocalDateTime.now();

        List<DeliveryEstimateResponse> options = methodRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(method -> rateRepository
                        .findByZoneIdAndMethodIdAndActiveTrue(zone.getId(), method.getId())
                        .map(rate -> buildEstimate(request, orderTime, zone, method, rate))
                        .orElse(null))
                .filter(option -> option != null)
                .toList();

        if (options.isEmpty()) {
            throw new BadRequestException(
                    "Khu vực '" + zone.getName() + "' chưa có phương thức giao hàng đang hoạt động.");
        }
        return options;
    }

    private DeliveryEstimateResponse buildEstimate(
            DeliveryEstimateRequest request,
            LocalDateTime orderTime,
            ShippingZone zone,
            ShippingMethod method,
            ShippingRate rate) {
        LocalDate baseDate = isPastCutoff(orderTime)
                ? orderTime.toLocalDate().plusDays(1)
                : orderTime.toLocalDate();

        boolean needsRestock = hasOutOfStockItem(request.getItems());
        int extraDays = needsRestock ? restockDelayDays : 0;
        LocalDate estimatedMinDate = addDaysSkippingHolidays(baseDate, rate.getMinDays() + extraDays);
        LocalDate estimatedMaxDate = addDaysSkippingHolidays(baseDate, rate.getMaxDays() + extraDays);

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

    private ShippingRate findActiveRate(ShippingZone zone, ShippingMethod method) {
        return rateRepository.findByZoneIdAndMethodIdAndActiveTrue(zone.getId(), method.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Khu vực '" + zone.getName() + "' chưa hỗ trợ phương thức " + method.getName()));
    }

    private ShippingZone findZone(String province) {
        String normalized = province == null ? "" : province.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException("Tỉnh/thành không được để trống.");
        }

        return zoneRepository.findByActiveTrueOrderByPriorityAsc().stream()
                .filter(zone -> zone.getProvinces().stream()
                        .anyMatch(value -> value.equalsIgnoreCase(normalized)))
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

    private LocalDate addDaysSkippingHolidays(LocalDate start, int days) {
        LocalDate date = start.plusDays(days);
        while (holidayRepository.existsByDate(date)) {
            date = date.plusDays(1);
        }
        return date;
    }
}
