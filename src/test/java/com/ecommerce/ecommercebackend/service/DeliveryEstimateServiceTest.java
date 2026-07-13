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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * C04 — Edge-case coverage for the delivery ETA calculation: order cutoff time,
 * holidays pushing the date forward, out-of-stock restock delay, and invalid
 * zone/method combinations.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryEstimateServiceTest {

    private static final int CUTOFF_HOUR = 18;
    private static final int RESTOCK_DELAY_DAYS = 3;

    @Mock
    private ShippingZoneRepository zoneRepository;

    @Mock
    private ShippingMethodRepository methodRepository;

    @Mock
    private ShippingRateRepository rateRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DeliveryEstimateService deliveryEstimateService;

    @BeforeEach
    void configureShippingRules() {
        ReflectionTestUtils.setField(deliveryEstimateService, "cutoffHour", CUTOFF_HOUR);
        ReflectionTestUtils.setField(deliveryEstimateService, "restockDelayDays", RESTOCK_DELAY_DAYS);
        lenient().when(holidayRepository.existsByDate(any())).thenReturn(false);
    }

    @Test
    void beforeCutoffUsesOrderDateAsBaseDate() {
        stubZoneMethodRate(zone("Hà Nội", 1), method("STANDARD"), rate(15_000L, 1, 2));
        LocalDateTime orderTime = LocalDate.of(2026, 7, 13).atTime(10, 0); // trước 18h

        DeliveryEstimateResponse response = deliveryEstimateService.estimate(
                request("Hà Nội", "STANDARD", List.of()), orderTime);

        assertThat(response.getEstimatedMinDate()).isEqualTo(LocalDate.of(2026, 7, 14));
        assertThat(response.getEstimatedMaxDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(response.getFee()).isEqualTo(15_000L);
        assertThat(response.getNote()).isNull();
    }

    @Test
    void afterCutoffPushesBaseDateToNextDay() {
        stubZoneMethodRate(zone("Hà Nội", 1), method("STANDARD"), rate(15_000L, 1, 2));
        LocalDateTime orderTime = LocalDate.of(2026, 7, 13).atTime(20, 0); // sau 18h

        DeliveryEstimateResponse response = deliveryEstimateService.estimate(
                request("Hà Nội", "STANDARD", List.of()), orderTime);

        // baseDate = 14/07 (đặt sau giờ chốt) => min = 15/07, max = 16/07
        assertThat(response.getEstimatedMinDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(response.getEstimatedMaxDate()).isEqualTo(LocalDate.of(2026, 7, 16));
    }

    @Test
    void deliveryDateFallingOnHolidayRollsToNextDay() {
        stubZoneMethodRate(zone("Hà Nội", 1), method("STANDARD"), rate(15_000L, 1, 1));
        LocalDateTime orderTime = LocalDate.of(2026, 7, 13).atTime(10, 0);
        // baseDate = 13/07, minDate = maxDate = 14/07 -> giả lập 14/07 là ngày nghỉ
        LocalDate holiday = LocalDate.of(2026, 7, 14);
        when(holidayRepository.existsByDate(holiday)).thenReturn(true);
        when(holidayRepository.existsByDate(holiday.plusDays(1))).thenReturn(false);

        DeliveryEstimateResponse response = deliveryEstimateService.estimate(
                request("Hà Nội", "STANDARD", List.of()), orderTime);

        assertThat(response.getEstimatedMinDate()).isEqualTo(holiday.plusDays(1));
        assertThat(response.getEstimatedMaxDate()).isEqualTo(holiday.plusDays(1));
    }

    @Test
    void outOfStockItemAddsRestockDelayToBothDates() {
        stubZoneMethodRate(zone("Hà Nội", 1), method("STANDARD"), rate(15_000L, 1, 2));
        LocalDateTime orderTime = LocalDate.of(2026, 7, 13).atTime(10, 0);

        Product outOfStock = Product.builder().id(10L).stockQuantity(0).reservedQuantity(0).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(outOfStock));

        DeliveryEstimateItemRequest item = new DeliveryEstimateItemRequest();
        item.setProductId(10L);
        item.setQuantity(1);

        DeliveryEstimateResponse response = deliveryEstimateService.estimate(
                request("Hà Nội", "STANDARD", List.of(item)), orderTime);

        assertThat(response.getEstimatedMinDate())
                .isEqualTo(LocalDate.of(2026, 7, 14).plusDays(RESTOCK_DELAY_DAYS));
        assertThat(response.getEstimatedMaxDate())
                .isEqualTo(LocalDate.of(2026, 7, 15).plusDays(RESTOCK_DELAY_DAYS));
        assertThat(response.getNote()).isNotBlank();
    }

    @Test
    void unknownProvinceThrowsBadRequest() {
        when(zoneRepository.findByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(zone("Hà Nội", 1)));

        assertThatThrownBy(() -> deliveryEstimateService.estimate(
                request("Cà Mau", "STANDARD", List.of()), LocalDateTime.now()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void methodWithoutRateInZoneThrowsBadRequest() {
        ShippingZone zone = zone("Hà Nội", 1);
        ShippingMethod expressMethod = method("EXPRESS");
        when(zoneRepository.findByActiveTrueOrderByPriorityAsc()).thenReturn(List.of(zone));
        when(methodRepository.findByCodeIgnoreCase("EXPRESS")).thenReturn(Optional.of(expressMethod));
        when(rateRepository.findByZoneIdAndMethodIdAndActiveTrue(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryEstimateService.estimate(
                request("Hà Nội", "EXPRESS", List.of()), LocalDateTime.now()))
                .isInstanceOf(BadRequestException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void stubZoneMethodRate(ShippingZone zone, ShippingMethod method, ShippingRate rate) {
        when(zoneRepository.findByActiveTrueOrderByPriorityAsc()).thenReturn(List.of(zone));
        when(methodRepository.findByCodeIgnoreCase(method.getCode())).thenReturn(Optional.of(method));
        when(rateRepository.findByZoneIdAndMethodIdAndActiveTrue(zone.getId(), method.getId()))
                .thenReturn(Optional.of(rate));
    }

    private ShippingZone zone(String province, int priority) {
        return ShippingZone.builder()
                .id(1L)
                .name(province + " zone")
                .provinces(java.util.Set.of(province))
                .priority(priority)
                .active(true)
                .build();
    }

    private ShippingMethod method(String code) {
        return ShippingMethod.builder().id(2L).code(code).name(code).active(true).build();
    }

    private ShippingRate rate(long fee, int minDays, int maxDays) {
        return ShippingRate.builder().fee(fee).minDays(minDays).maxDays(maxDays).active(true).build();
    }

    private DeliveryEstimateRequest request(
            String province, String methodCode, List<DeliveryEstimateItemRequest> items) {
        DeliveryEstimateRequest request = new DeliveryEstimateRequest();
        request.setProvince(province);
        request.setMethodCode(methodCode);
        request.setItems(items);
        return request;
    }
}
