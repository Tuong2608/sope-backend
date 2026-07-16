package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.ShippingMethod;
import com.ecommerce.ecommercebackend.entity.ShippingRate;
import com.ecommerce.ecommercebackend.entity.ShippingZone;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ShippingMethodRepository;
import com.ecommerce.ecommercebackend.repository.ShippingRateRepository;
import com.ecommerce.ecommercebackend.repository.ShippingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminShippingService {

    private final ShippingMethodRepository methodRepository;
    private final ShippingZoneRepository zoneRepository;
    private final ShippingRateRepository rateRepository;

    @Transactional(readOnly = true)
    public List<MethodResponse> getMethods() {
        return methodRepository.findAll().stream()
                .sorted(Comparator.comparing(ShippingMethod::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toMethodResponse)
                .toList();
    }

    @Transactional
    public MethodResponse createMethod(String code, String name, boolean active) {
        String normalizedCode = normalizeCode(code);
        if (methodRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BadRequestException("Mã phương thức giao hàng đã tồn tại: " + normalizedCode);
        }

        ShippingMethod method = ShippingMethod.builder()
                .code(normalizedCode)
                .name(normalizeRequiredText(name, "Tên phương thức"))
                .active(active)
                .build();
        return toMethodResponse(methodRepository.save(method));
    }

    @Transactional
    public MethodResponse updateMethod(Long id, String code, String name, boolean active) {
        ShippingMethod method = findMethod(id);
        String normalizedCode = normalizeCode(code);
        if (methodRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new BadRequestException("Mã phương thức giao hàng đã tồn tại: " + normalizedCode);
        }

        method.setCode(normalizedCode);
        method.setName(normalizeRequiredText(name, "Tên phương thức"));
        method.setActive(active);
        return toMethodResponse(methodRepository.save(method));
    }

    @Transactional
    public MethodResponse setMethodActive(Long id, boolean active) {
        ShippingMethod method = findMethod(id);
        method.setActive(active);
        return toMethodResponse(methodRepository.save(method));
    }

    @Transactional
    public void deleteMethod(Long id) {
        ShippingMethod method = findMethod(id);
        if (rateRepository.countByMethodId(id) > 0) {
            throw new BadRequestException(
                    "Không thể xóa phương thức đang được sử dụng trong bảng giá. Hãy xóa bảng giá liên quan trước.");
        }
        methodRepository.delete(method);
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getZones() {
        return zoneRepository.findAll().stream()
                .sorted(Comparator.comparingInt(ShippingZone::getPriority)
                        .thenComparing(ShippingZone::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toZoneResponse)
                .toList();
    }

    @Transactional
    public ZoneResponse createZone(String name, Set<String> provinces, int priority, boolean active) {
        String normalizedName = normalizeRequiredText(name, "Tên khu vực");
        if (zoneRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BadRequestException("Tên khu vực giao hàng đã tồn tại: " + normalizedName);
        }

        ShippingZone zone = ShippingZone.builder()
                .name(normalizedName)
                .provinces(normalizeProvinces(provinces))
                .priority(priority)
                .active(active)
                .build();
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public ZoneResponse updateZone(
            Long id, String name, Set<String> provinces, int priority, boolean active) {
        ShippingZone zone = findZone(id);
        String normalizedName = normalizeRequiredText(name, "Tên khu vực");
        if (zoneRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new BadRequestException("Tên khu vực giao hàng đã tồn tại: " + normalizedName);
        }

        zone.setName(normalizedName);
        zone.setProvinces(normalizeProvinces(provinces));
        zone.setPriority(priority);
        zone.setActive(active);
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public ZoneResponse setZoneActive(Long id, boolean active) {
        ShippingZone zone = findZone(id);
        zone.setActive(active);
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(Long id) {
        ShippingZone zone = findZone(id);
        if (rateRepository.countByZoneId(id) > 0) {
            throw new BadRequestException(
                    "Không thể xóa khu vực đang được sử dụng trong bảng giá. Hãy xóa bảng giá liên quan trước.");
        }
        zoneRepository.delete(zone);
    }

    @Transactional(readOnly = true)
    public List<RateResponse> getRates() {
        return rateRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((ShippingRate rate) -> rate.getZone().getPriority())
                        .thenComparing(rate -> rate.getZone().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(rate -> rate.getMethod().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toRateResponse)
                .toList();
    }

    @Transactional
    public RateResponse createRate(
            Long zoneId, Long methodId, Long fee, int minDays, int maxDays, boolean active) {
        validateRateWindow(minDays, maxDays);
        if (rateRepository.existsByZoneIdAndMethodId(zoneId, methodId)) {
            throw new BadRequestException("Bảng giá cho khu vực và phương thức này đã tồn tại.");
        }

        ShippingRate rate = ShippingRate.builder()
                .zone(findZone(zoneId))
                .method(findMethod(methodId))
                .fee(fee)
                .minDays(minDays)
                .maxDays(maxDays)
                .active(active)
                .build();
        return toRateResponse(rateRepository.save(rate));
    }

    @Transactional
    public RateResponse updateRate(
            Long id, Long zoneId, Long methodId, Long fee, int minDays, int maxDays, boolean active) {
        validateRateWindow(minDays, maxDays);
        if (rateRepository.existsByZoneIdAndMethodIdAndIdNot(zoneId, methodId, id)) {
            throw new BadRequestException("Bảng giá cho khu vực và phương thức này đã tồn tại.");
        }

        ShippingRate rate = findRate(id);
        rate.setZone(findZone(zoneId));
        rate.setMethod(findMethod(methodId));
        rate.setFee(fee);
        rate.setMinDays(minDays);
        rate.setMaxDays(maxDays);
        rate.setActive(active);
        return toRateResponse(rateRepository.save(rate));
    }

    @Transactional
    public RateResponse setRateActive(Long id, boolean active) {
        ShippingRate rate = findRate(id);
        rate.setActive(active);
        return toRateResponse(rateRepository.save(rate));
    }

    @Transactional
    public void deleteRate(Long id) {
        rateRepository.delete(findRate(id));
    }

    private ShippingMethod findMethod(Long id) {
        return methodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found: " + id));
    }

    private ShippingZone findZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found: " + id));
    }

    private ShippingRate findRate(Long id) {
        return rateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping rate not found: " + id));
    }

    private String normalizeCode(String code) {
        return normalizeRequiredText(code, "Mã phương thức")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_-]", "_");
    }

    private String normalizeRequiredText(String value, String label) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new BadRequestException(label + " không được để trống.");
        }
        return normalized;
    }

    private Set<String> normalizeProvinces(Set<String> provinces) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (provinces != null) {
            provinces.stream()
                    .map(value -> value == null ? "" : value.trim().replaceAll("\\s+", " "))
                    .filter(value -> !value.isEmpty())
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            throw new BadRequestException("Khu vực phải có ít nhất một tỉnh/thành.");
        }
        return normalized;
    }

    private void validateRateWindow(int minDays, int maxDays) {
        if (minDays > maxDays) {
            throw new BadRequestException("Số ngày giao tối thiểu không được lớn hơn số ngày tối đa.");
        }
    }

    private MethodResponse toMethodResponse(ShippingMethod method) {
        return new MethodResponse(method.getId(), method.getCode(), method.getName(), method.isActive());
    }

    private ZoneResponse toZoneResponse(ShippingZone zone) {
        return new ZoneResponse(zone.getId(), zone.getName(), Set.copyOf(zone.getProvinces()),
                zone.getPriority(), zone.isActive());
    }

    private RateResponse toRateResponse(ShippingRate rate) {
        return new RateResponse(rate.getId(), toZoneResponse(rate.getZone()),
                toMethodResponse(rate.getMethod()), rate.getFee(), rate.getMinDays(),
                rate.getMaxDays(), rate.isActive());
    }

    public record MethodResponse(Long id, String code, String name, boolean active) {}
    public record ZoneResponse(Long id, String name, Set<String> provinces, int priority, boolean active) {}
    public record RateResponse(Long id, ZoneResponse zone, MethodResponse method, Long fee,
                               int minDays, int maxDays, boolean active) {}
}
