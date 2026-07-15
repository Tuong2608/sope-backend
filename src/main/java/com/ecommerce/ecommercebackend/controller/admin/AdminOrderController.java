package com.ecommerce.ecommercebackend.controller.admin;

import com.ecommerce.ecommercebackend.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.ecommercebackend.dto.response.OrderResponse;
import com.ecommerce.ecommercebackend.dto.response.PagedResponse;
import com.ecommerce.ecommercebackend.entity.OrderStatus;
import com.ecommerce.ecommercebackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Admin order management (task H03: search/filter/detail/status/tracking).
 * Secured to ROLE_ADMIN via {@code /api/admin/**} in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Searches/filters orders, paginated.
     *
     * @param keyword matches order code, recipient name or phone
     * @param status  exact status filter
     * @param from    creation date lower bound (inclusive)
     * @param to      creation date upper bound (inclusive)
     */
    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> all(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size < 1 ? 20 : Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(orderService.searchOrders(keyword, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> one(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getAnyOrder(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus()));
    }
}
