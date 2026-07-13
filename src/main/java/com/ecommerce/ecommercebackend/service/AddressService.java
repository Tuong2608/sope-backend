package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.request.AddressRequest;
import com.ecommerce.ecommercebackend.dto.response.AddressResponse;
import com.ecommerce.ecommercebackend.entity.Address;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for a user's delivery address book (task C01).
 *
 * <p>At most one address per user is flagged {@code isDefault}; this service
 * keeps that invariant when addresses are created, updated or deleted.</p>
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    // ── Read ────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(User user) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getOne(User user, Long id) {
        return toResponse(findOwnedOrThrow(user, id));
    }

    // ── Create ──────────────────────────────────────────────────────────────────

    @Transactional
    public AddressResponse create(User user, AddressRequest request) {
        boolean isFirstAddress = addressRepository.countByUserId(user.getId()) == 0;
        boolean shouldBeDefault = isFirstAddress || request.isDefault();

        if (shouldBeDefault) {
            clearExistingDefault(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .isDefault(shouldBeDefault)
                .build();
        applyRequest(address, request);

        return toResponse(addressRepository.save(address));
    }

    // ── Update ──────────────────────────────────────────────────────────────────

    @Transactional
    public AddressResponse update(User user, Long id, AddressRequest request) {
        Address address = findOwnedOrThrow(user, id);

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(user.getId());
            address.setDefault(true);
        }
        applyRequest(address, request);

        return toResponse(addressRepository.save(address));
    }

    /** Sets one address as the default, unsetting whichever one was default before. */
    @Transactional
    public AddressResponse setDefault(User user, Long id) {
        Address address = findOwnedOrThrow(user, id);
        if (!address.isDefault()) {
            clearExistingDefault(user.getId());
            address.setDefault(true);
            addressRepository.save(address);
        }
        return toResponse(address);
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(User user, Long id) {
        Address address = findOwnedOrThrow(user, id);
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            // Promote the most recently created remaining address, if any.
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        addressRepository.save(next);
                    });
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private Address findOwnedOrThrow(User user, Long id) {
        return addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + id));
    }

    private void clearExistingDefault(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(current -> {
                    current.setDefault(false);
                    addressRepository.save(current);
                });
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setAddressDetail(request.getAddressDetail());
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .addressDetail(address.getAddressDetail())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
