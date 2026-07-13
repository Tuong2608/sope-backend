# 📖 Tài liệu API – SOPE Backend

> **Phiên bản:** 1.0 · **Cập nhật:** 13/07/2026  
> **Base URL:** `http://localhost:8080` (dev) · `https://api.sope.vn` (prod)  
> **Auth:** Bearer JWT trong header `Authorization: Bearer <token>`

---

## Mục lục

1. [Auth – Xác thực](#1-auth--xác-thực)
2. [Product – Sản phẩm](#2-product--sản-phẩm)
3. [Product Variant – Phiên bản](#3-product-variant--phiên-bản)
4. [Cart – Giỏ hàng](#4-cart--giỏ-hàng)
5. [Order – Đơn hàng](#5-order--đơn-hàng)
6. [Inventory – Tồn kho](#6-inventory--tồn-kho)
7. [Admin – Quản trị](#7-admin--quản-trị)
8. [Mẫu lỗi](#8-mẫu-lỗi)

---

## 1. Auth – Xác thực

### POST `/api/auth/register`
Đăng ký tài khoản mới.

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "Abc123!@#",
  "fullName": "Nguyễn Văn A"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGci...",
  "email": "user@example.com",
  "fullName": "Nguyễn Văn A",
  "role": "USER"
}
```

---

### POST `/api/auth/login`
Đăng nhập, lấy JWT.

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "Abc123!@#"
}
```

**Response 200:** (giống register)

---

## 2. Product – Sản phẩm

> ⚠️ Sản phẩm có `status = INACTIVE` bị ẩn khỏi API public.

### GET `/api/products`
Lấy danh sách sản phẩm (có phân trang + bộ lọc).

**Query params:**

| Param | Kiểu | Mô tả | Ví dụ |
|-------|------|--------|-------|
| `keyword` | string | Tìm theo tên | `iphone` |
| `category` | string | Lọc theo loại | `laptop`, `dien-thoai` |
| `brand` | string | Lọc theo hãng | `Apple` |
| `minPrice` | long | Giá tối thiểu (VND) | `5000000` |
| `maxPrice` | long | Giá tối đa (VND) | `30000000` |
| `inStock` | boolean | Chỉ hiện hàng còn | `true` |
| `page` | int | Trang (bắt đầu 0) | `0` |
| `size` | int | Kích thước trang | `20` |
| `sort` | string | Sắp xếp | `price,asc` |

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "sku": "LAPTOP-001",
      "name": "MacBook Air M2 2024",
      "category": "laptop",
      "brand": "Apple",
      "price": 27990000,
      "oldPrice": 30790000,
      "imgUrl": "/images/macbook-air.jpg",
      "status": "ACTIVE",
      "availableQuantity": 45,
      "inStock": true,
      "lowStock": false,
      "variants": []
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "page": 0,
  "size": 20
}
```

---

### GET `/api/products/{id}`
Lấy chi tiết một sản phẩm.

**Response 200:**
```json
{
  "id": 1,
  "sku": "LAPTOP-001",
  "name": "MacBook Air M2 2024",
  "category": "laptop",
  "brand": "Apple",
  "shortDescription": "Laptop mỏng nhẹ, chip M2...",
  "price": 27990000,
  "oldPrice": 30790000,
  "imgUrl": "https://...",
  "images": ["https://img1.jpg", "https://img2.jpg"],
  "specs": {
    "CPU": "Apple M2",
    "RAM": "8GB",
    "SSD": "256GB"
  },
  "status": "ACTIVE",
  "availableQuantity": 45,
  "inStock": true,
  "lowStock": false,
  "variants": [
    {
      "id": 10,
      "sku": "LAPTOP-001-SILVER-256GB",
      "colorName": "Bạc",
      "colorHex": "#C0C0C0",
      "storageName": "256GB",
      "price": 27990000,
      "stockQuantity": 20,
      "availableQuantity": 18,
      "inStock": true
    }
  ]
}
```

**Response 404:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 999"
}
```

---

## 3. Product Variant – Phiên bản

### GET `/api/products/{productId}/variants`
Lấy danh sách variant đang active + còn hàng.

**Response 200:**
```json
[
  {
    "id": 10,
    "sku": "LAPTOP-001-SILVER-256GB",
    "colorName": "Bạc",
    "colorHex": "#C0C0C0",
    "storageName": "256GB",
    "price": 27990000,
    "imageUrl": "https://img-silver.jpg",
    "stockQuantity": 20,
    "reservedQuantity": 2,
    "availableQuantity": 18,
    "active": true,
    "inStock": true
  }
]
```

---

## 4. Cart – Giỏ hàng

> 🔒 Yêu cầu đăng nhập (Bearer token).

### GET `/api/cart`
Lấy giỏ hàng của user hiện tại.

**Response 200:**
```json
{
  "id": 5,
  "items": [
    {
      "id": 12,
      "productId": 1,
      "name": "MacBook Air M2 2024",
      "imgUrl": "https://img-silver.jpg",
      "price": 27990000,
      "quantity": 1,
      "lineTotal": 27990000
    }
  ],
  "totalItems": 1,
  "totalAmount": 27990000
}
```

---

### POST `/api/cart/items`
Thêm sản phẩm vào giỏ. B05: kèm `variantId` để chọn màu/dung lượng.

**Request body:**
```json
{
  "productId": 1,
  "variantId": 10,
  "quantity": 1
}
```

> Lỗi khi vượt tồn kho:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Chỉ còn 2 sản phẩm trong kho"
}
```

---

### PUT `/api/cart/items/{itemId}`
Cập nhật số lượng.

**Request body:** `{ "quantity": 2 }`

---

### DELETE `/api/cart/items/{itemId}`
Xoá một dòng khỏi giỏ.

---

## 5. Order – Đơn hàng

> 🔒 Yêu cầu đăng nhập.

### POST `/api/orders`
Đặt hàng từ giỏ hàng hiện tại.

**Request body:**
```json
{
  "shippingAddress": "123 Nguyễn Huệ, Q1, TP.HCM",
  "paymentMethod": "VNPAY"
}
```

**Response 201:**
```json
{
  "orderId": 55,
  "status": "PENDING",
  "totalAmount": 27990000,
  "paymentUrl": "https://sandbox.vnpayment.vn/pay?vnp_..."
}
```

---

### GET `/api/orders`
Lịch sử đơn hàng của user.

---

### GET `/api/orders/{id}`
Chi tiết một đơn hàng.

---

## 6. Inventory – Tồn kho

> 🔒 Yêu cầu đăng nhập.

### POST `/api/inventory/reserve`
B07: Giữ hàng 15 phút khi bắt đầu checkout.

**Request body:**
```json
{
  "productId": 1,
  "variantId": 10,
  "quantity": 1
}
```

**Response 200:**
```json
{
  "reservationId": 77,
  "productId": 1,
  "variantId": 10,
  "quantity": 1,
  "expiresAt": "2026-07-13T12:15:00",
  "message": "Đã giữ hàng thành công. Vui lòng thanh toán trước 2026-07-13T12:15:00"
}
```

> ⏰ Frontend cần đếm ngược đến `expiresAt`. Nếu hết giờ, hàng được trả về kho tự động.

---

### POST `/api/inventory/release/{reservationId}`
Giải phóng reservation (user huỷ checkout).

**Response 200:** `"Đã giải phóng reservation #77"`

---

## 7. Admin – Quản trị

> 🔒 Yêu cầu role `ADMIN`.

### GET `/api/admin/products/validate`
A04: Kiểm tra dữ liệu sản phẩm.

**Response 200:**
```json
{
  "totalProducts": 150,
  "duplicateSkus": [],
  "missingPriceIds": [5, 12],
  "missingImageIds": [],
  "missingBrandIds": [7],
  "invalidCategoryIds": [],
  "hasErrors": true,
  "errorCount": 3
}
```

---

### POST `/api/admin/products/import/laptop`
A03: Import laptop từ file crawl TGDD.

**Request body:** JSON array theo schema crawl.

**Response 200:**
```json
{
  "totalReceived": 200,
  "imported": 185,
  "skipped": 15,
  "skippedReasons": ["SKU=363417 | SKU đã tồn tại"]
}
```

---

### POST `/api/admin/products/{productId}/variants`
B02: Tạo variant mới.

**Request body:**
```json
{
  "colorName": "Bạc",
  "colorHex": "#C0C0C0",
  "storageName": "256GB",
  "price": 27990000,
  "imageUrl": "https://img.jpg",
  "stockQuantity": 50,
  "active": true
}
```

---

### PUT `/api/admin/variants/{variantId}`
Cập nhật variant.

### DELETE `/api/admin/variants/{variantId}`
Deactivate (xoá mềm) variant.

---

## 8. Mẫu lỗi

Tất cả lỗi đều trả theo format chuẩn:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Mô tả lỗi cụ thể",
  "path": "/api/cart/items",
  "timestamp": "2026-07-13T10:00:00"
}
```

| HTTP Code | Ý nghĩa |
|-----------|---------|
| `400` | Dữ liệu request không hợp lệ |
| `401` | Chưa đăng nhập / token hết hạn |
| `403` | Không có quyền truy cập |
| `404` | Không tìm thấy resource |
| `409` | Xung đột dữ liệu (VD: email đã tồn tại) |
| `500` | Lỗi server nội bộ |

---

## 📝 Lưu ý cho Frontend

- Trường `inStock: false` → disable nút "Thêm vào giỏ"
- Trường `lowStock: true` → hiện badge "Sắp hết hàng"
- Khi checkout: gọi `/api/inventory/reserve` trước, lấy `expiresAt` để đếm ngược
- Token JWT hết hạn → redirect về `/login`
- Các sản phẩm `status = INACTIVE` sẽ không xuất hiện trong kết quả tìm kiếm

## 📝 Lưu ý cho Chatbot

- Dùng `GET /api/products?category=laptop&inStock=true` để lấy laptop còn hàng
- Field `availableQuantity` là số lượng thực có thể mua
- Không trả thông tin nhạy cảm về tài khoản user cho chatbot
