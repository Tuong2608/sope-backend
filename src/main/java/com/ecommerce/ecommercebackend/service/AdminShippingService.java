package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.entity.ShippingMethod;
import com.ecommerce.ecommercebackend.entity.ShippingRate;
import com.ecommerce.ecommercebackend.entity.ShippingZone;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ShippingMethodRepository;
import com.ecommerce.ecommercebackend.repository.ShippingRateRepository;
import com.ecommerce.ecommercebackend.repository.ShippingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminShippingService {

    private final ShippingMethodRepository methodRepository;
    private final ShippingZoneRepository zoneRepository;
    private final ShippingRateRepository rateRepository;

    @Transactional(readOnly = true)
    public List<MethodResponse> getMethods() {
        return methodRepository.findAll().stream().map(this::toMethodResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getZones() {
        return zoneRepository.findAll().stream().map(this::toZoneResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RateResponse> getRates() {
        return rateRepository.findAll().stream().map(this::toRateResponse).toList();
    }

    @Transactional
    public MethodResponse setMethodActive(Long id, boolean active) {
        ShippingMethod method = methodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found: " + id));
        method.setActive(active);
        return toMethodResponse(methodRepository.save(method));
    }

    @Transactional
    public ZoneResponse setZoneActive(Long id, boolean active) {
        ShippingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found: " + id));
        zone.setActive(active);
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public RateResponse setRateActive(Long id, boolean active) {
        ShippingRate rate = rateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping rate not found: " + id));
        rate.setActive(active);
        return toRateResponse(rateRepository.save(rate));
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
