package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.AddressRequest;
import com.ecommerce.ecommercebackend.dto.response.AddressResponse;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the authenticated user's delivery address book (task C01).
 * All routes require authentication (covered by {@code anyRequest().authenticated()}
 * in {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> myAddresses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(addressService.getMyAddresses(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getOne(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.getOne(user, id));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse created = addressService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(user, id, request));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefault(user, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        addressService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
