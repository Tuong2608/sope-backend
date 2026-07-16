package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.service.AdminShippingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShippingController {

    private final AdminShippingService shippingService;

    public record MethodRequest(
            @NotBlank(message = "code is required")
            @Size(max = 30, message = "code must not exceed 30 characters")
            String code,
            @NotBlank(message = "name is required")
            @Size(max = 100, message = "name must not exceed 100 characters")
            String name,
            boolean active
    ) {}

    public record ZoneRequest(
            @NotBlank(message = "name is required")
            @Size(max = 100, message = "name must not exceed 100 characters")
            String name,
            @NotEmpty(message = "provinces must contain at least one province")
            Set<@NotBlank String> provinces,
            @Min(value = 0, message = "priority cannot be negative")
            int priority,
            boolean active
    ) {}

    public record RateRequest(
            @NotNull(message = "zoneId is required") Long zoneId,
            @NotNull(message = "methodId is required") Long methodId,
            @NotNull(message = "fee is required")
            @Min(value = 0, message = "fee cannot be negative") Long fee,
            @Min(value = 0, message = "minDays cannot be negative") int minDays,
            @Min(value = 0, message = "maxDays cannot be negative") int maxDays,
            boolean active
    ) {}

    @GetMapping("/methods")
    public ResponseEntity<List<AdminShippingService.MethodResponse>> methods() {
        return ResponseEntity.ok(shippingService.getMethods());
    }

    @PostMapping("/methods")
    public ResponseEntity<AdminShippingService.MethodResponse> createMethod(
            @Valid @RequestBody MethodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                shippingService.createMethod(request.code(), request.name(), request.active()));
    }

    @PutMapping("/methods/{id}")
    public ResponseEntity<AdminShippingService.MethodResponse> updateMethod(
            @PathVariable Long id,
            @Valid @RequestBody MethodRequest request) {
        return ResponseEntity.ok(
                shippingService.updateMethod(id, request.code(), request.name(), request.active()));
    }

    @PatchMapping("/methods/{id}/active")
    public ResponseEntity<AdminShippingService.MethodResponse> setMethodActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(shippingService.setMethodActive(id, active));
    }

    @DeleteMapping("/methods/{id}")
    public ResponseEntity<Void> deleteMethod(@PathVariable Long id) {
        shippingService.deleteMethod(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/zones")
    public ResponseEntity<List<AdminShippingService.ZoneResponse>> zones() {
        return ResponseEntity.ok(shippingService.getZones());
    }

    @PostMapping("/zones")
    public ResponseEntity<AdminShippingService.ZoneResponse> createZone(
            @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                shippingService.createZone(
                        request.name(), request.provinces(), request.priority(), request.active()));
    }

    @PutMapping("/zones/{id}")
    public ResponseEntity<AdminShippingService.ZoneResponse> updateZone(
            @PathVariable Long id,
            @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(shippingService.updateZone(
                id, request.name(), request.provinces(), request.priority(), request.active()));
    }

    @PatchMapping("/zones/{id}/active")
    public ResponseEntity<AdminShippingService.ZoneResponse> setZoneActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(shippingService.setZoneActive(id, active));
    }

    @DeleteMapping("/zones/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        shippingService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rates")
    public ResponseEntity<List<AdminShippingService.RateResponse>> rates() {
        return ResponseEntity.ok(shippingService.getRates());
    }

    @PostMapping("/rates")
    public ResponseEntity<AdminShippingService.RateResponse> createRate(
            @Valid @RequestBody RateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.createRate(
                request.zoneId(), request.methodId(), request.fee(), request.minDays(),
                request.maxDays(), request.active()));
    }

    @PutMapping("/rates/{id}")
    public ResponseEntity<AdminShippingService.RateResponse> updateRate(
            @PathVariable Long id,
            @Valid @RequestBody RateRequest request) {
        return ResponseEntity.ok(shippingService.updateRate(
                id, request.zoneId(), request.methodId(), request.fee(), request.minDays(),
                request.maxDays(), request.active()));
    }

    @PatchMapping("/rates/{id}/active")
    public ResponseEntity<AdminShippingService.RateResponse> setRateActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(shippingService.setRateActive(id, active));
    }

    @DeleteMapping("/rates/{id}")
    public ResponseEntity<Void> deleteRate(@PathVariable Long id) {
        shippingService.deleteRate(id);
        return ResponseEntity.noContent().build();
    }
}
