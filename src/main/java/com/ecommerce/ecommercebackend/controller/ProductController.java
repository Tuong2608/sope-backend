package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.dto.request.ProductRequest;
import com.ecommerce.ecommercebackend.dto.response.PagedResponse;
import com.ecommerce.ecommercebackend.dto.response.ProductResponse;
import com.ecommerce.ecommercebackend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * REST controller exposing the product catalog.
 *
 * <p>Reads ({@code GET}) are public; writes ({@code POST}/{@code PUT}/
 * {@code DELETE}) require authentication (see {@code SecurityConfig}).</p>
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    /** Sortable fields, whitelisted to reject arbitrary/invalid property names. */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "sku", "name", "price", "oldPrice", "category", "brand");

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Read (single) ───────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    // ── Read (list: search + filter + paginate) ─────────────────────────────────

    /**
     * Lists products with optional keyword search, attribute filters,
     * pagination and sorting.
     *
     * @param keyword  case-insensitive substring matched against the name
     * @param category exact category match (e.g. "Điện thoại")
     * @param brand    case-insensitive substring matched against the brand
     * @param minPrice lower price bound in VND (inclusive)
     * @param maxPrice upper price bound in VND (inclusive)
     * @param page     zero-based page index
     * @param size     page size
     * @param sortBy   field to sort by (whitelisted; defaults to "id")
     * @param sortDir  sort direction, "asc" or "desc"
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String storage,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        String sortField = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size < 1 ? 10 : Math.min(size, 100),
                Sort.by(direction, sortField));

        PagedResponse<ProductResponse> result = productService.search(
                keyword, category, brand, storage, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(result);
    }

    // ── Update ──────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
