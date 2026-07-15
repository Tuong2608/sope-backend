package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.response.ShipmentTrackingResponse;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.ShipmentStatus;
import com.ecommerce.ecommercebackend.entity.ShipmentTracking;
import com.ecommerce.ecommercebackend.exception.BadRequestException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.ShipmentTrackingEventRepository;
import com.ecommerce.ecommercebackend.repository.ShipmentTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * C10 — Coverage for the simulated shipment carrier: idempotent creation,
 * linear status advancement, and refusing to advance past DELIVERED.
 */
@ExtendWith(MockitoExtension.class)
class ShipmentTrackingServiceTest {

    @Mock
    private ShipmentTrackingRepository trackingRepository;

    @Mock
    private ShipmentTrackingEventRepository eventRepository;

    @InjectMocks
    private ShipmentTrackingService shipmentTrackingService;

    @Test
    void createForOrderIsIdempotent() {
        Order order = Order.builder().id(1L).build();
        ShipmentTracking existing = ShipmentTracking.builder()
                .id(9L).order(order).trackingNumber("SPX1234567890").status(ShipmentStatus.CREATED).build();
        when(trackingRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        ShipmentTracking result = shipmentTrackingService.createForOrder(order);

        assertThat(result).isEqualTo(existing);
        verify(trackingRepository, never()).save(any(ShipmentTracking.class));
    }

    @Test
    void createForOrderGeneratesTrackingNumberWhenAbsent() {
        Order order = Order.builder().id(1L).build();
        when(trackingRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(trackingRepository.existsByTrackingNumber(anyString())).thenReturn(false);
        when(trackingRepository.save(any(ShipmentTracking.class))).thenAnswer(inv -> {
            ShipmentTracking t = inv.getArgument(0);
            t.setId(9L);
            return t;
        });

        ShipmentTracking result = shipmentTrackingService.createForOrder(order);

        assertThat(result.getTrackingNumber()).startsWith("SPX");
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        verify(eventRepository).save(any());
    }

    @Test
    void advanceMovesToNextStatusInSequence() {
        Order order = Order.builder().id(1L).build();
        ShipmentTracking tracking = ShipmentTracking.builder()
                .id(9L).order(order).trackingNumber("SPX1234567890").status(ShipmentStatus.CREATED).build();
        when(trackingRepository.findByOrderId(1L)).thenReturn(Optional.of(tracking));
        when(trackingRepository.save(tracking)).thenReturn(tracking);
        when(eventRepository.findByTrackingIdOrderByCreatedAtAsc(9L)).thenReturn(List.of());

        ShipmentTrackingResponse response = shipmentTrackingService.advance(1L);

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(tracking.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
    }

    @Test
    void advanceAfterDeliveredThrowsBadRequest() {
        Order order = Order.builder().id(1L).build();
        ShipmentTracking tracking = ShipmentTracking.builder()
                .id(9L).order(order).trackingNumber("SPX1234567890").status(ShipmentStatus.DELIVERED).build();
        when(trackingRepository.findByOrderId(1L)).thenReturn(Optional.of(tracking));

        assertThatThrownBy(() -> shipmentTrackingService.advance(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void advanceWithoutTrackingThrowsNotFound() {
        when(trackingRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentTrackingService.advance(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
