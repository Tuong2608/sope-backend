# 📡 API Reference

**Base URL:** `http://localhost:8080`

> Các API có ký hiệu 🔒 yêu cầu **JWT Token** trong header.  
> Xem hướng dẫn lấy token tại mục [Authentication](#-authentication).

---

## 🔐 Authentication

### POST `/api/auth/register` — Đăng ký tài khoản

**Không cần xác thực.**

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Response `201 Created`:**
```
User registered successfully.
```

**Lỗi có thể gặp:**

| Status | Mô tả |
|--------|-------|
| `400` | Dữ liệu không hợp lệ (thiếu trường, sai định dạng) |
| `409` | Username hoặc email đã tồn tại |

---

### POST `/api/auth/login` — Đăng nhập

**Không cần xác thực.**

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

**Response `200 OK`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTcxNzAwMDAwMCwiZXhwIjoxNzE3MDg2NDAwfQ.abc123...",
  "tokenType": "Bearer"
}
```

> ⏰ **Token có hiệu lực trong 24 giờ** (86400000ms). Sau đó cần đăng nhập lại.

**Lỗi có thể gặp:**

| Status | Mô tả |
|--------|-------|
| `400` | Thiếu username hoặc password |
| `401` | Sai username hoặc password |

---

## 🔑 Cách dùng JWT Token

Sau khi đăng nhập, copy `accessToken` và thêm vào **header** của mỗi request cần xác thực:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Trong Postman:
1. Chọn request muốn gọi
2. Vào tab **Authorization**
3. Chọn Type: **Bearer Token**
4. Dán token vào ô **Token**

### Trong code (ví dụ với fetch):
```javascript
fetch('http://localhost:8080/api/products', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9...'
  },
  body: JSON.stringify({ name: 'iPhone 15', ... })
})
```

---

## 📦 Products

### GET `/api/products` — Lấy danh sách sản phẩm

**Không cần xác thực.**

**Query Parameters (tất cả đều tuỳ chọn):**

| Tham số | Kiểu | Mô tả | Mặc định |
|---------|------|-------|----------|
| `keyword` | String | Tìm kiếm theo tên sản phẩm (không phân biệt hoa thường) | - |
| `category` | String | Lọc chính xác theo danh mục (vd: `Điện thoại`) | - |
| `brand` | String | Lọc theo thương hiệu (không phân biệt hoa thường) | - |
| `minPrice` | Long | Giá tối thiểu (VND, tính theo đơn vị đồng) | - |
| `maxPrice` | Long | Giá tối đa (VND) | - |
| `page` | int | Số trang, bắt đầu từ `0` | `0` |
| `size` | int | Số sản phẩm mỗi trang (tối đa `100`) | `10` |
| `sortBy` | String | Sắp xếp theo: `id` / `name` / `price` / `oldPrice` / `category` / `brand` | `id` |
| `sortDir` | String | Chiều sắp xếp: `asc` hoặc `desc` | `asc` |

**Ví dụ request:**
```
GET http://localhost:8080/api/products?keyword=iphone&category=Điện thoại&minPrice=10000000&maxPrice=50000000&page=0&size=5&sortBy=price&sortDir=asc
```

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "iPhone 15 Pro Max",
      "category": "Điện thoại",
      "brand": "Apple",
      "price": 34990000,
      "oldPrice": 37990000,
      "description": "iPhone 15 Pro Max 256GB"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### GET `/api/products/{id}` — Lấy sản phẩm theo ID

**Không cần xác thực.**

**Ví dụ:**
```
GET http://localhost:8080/api/products/1
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "name": "iPhone 15 Pro Max",
  "category": "Điện thoại",
  "brand": "Apple",
  "price": 34990000,
  "oldPrice": 37990000,
  "description": "iPhone 15 Pro Max 256GB"
}
```

**Lỗi:**

| Status | Mô tả |
|--------|-------|
| `404` | Không tìm thấy sản phẩm với ID này |

---

### POST `/api/products` — Tạo sản phẩm mới 🔒

**Yêu cầu JWT Token.**

**Request Body:**
```json
{
  "name": "iPhone 15 Pro Max",
  "category": "Điện thoại",
  "brand": "Apple",
  "price": 34990000,
  "oldPrice": 37990000,
  "description": "iPhone 15 Pro Max 256GB"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "name": "iPhone 15 Pro Max",
  "category": "Điện thoại",
  "brand": "Apple",
  "price": 34990000,
  "oldPrice": 37990000,
  "description": "iPhone 15 Pro Max 256GB"
}
```

---

### PUT `/api/products/{id}` — Cập nhật sản phẩm 🔒

**Yêu cầu JWT Token.**

**Ví dụ:**
```
PUT http://localhost:8080/api/products/1
```

**Request Body** (gửi đầy đủ các trường cần cập nhật):
```json
{
  "name": "iPhone 15 Pro Max 512GB",
  "category": "Điện thoại",
  "brand": "Apple",
  "price": 39990000,
  "oldPrice": 42990000,
  "description": "iPhone 15 Pro Max 512GB - Titanium"
}
```

**Response `200 OK`:** Trả về sản phẩm sau khi cập nhật.

---

### DELETE `/api/products/{id}` — Xoá sản phẩm 🔒

**Yêu cầu JWT Token.**

**Ví dụ:**
```
DELETE http://localhost:8080/api/products/1
```

**Response `204 No Content`:** Xoá thành công, không trả về body.

**Lỗi:**

| Status | Mô tả |
|--------|-------|
| `404` | Không tìm thấy sản phẩm với ID này |

---

## 🛠️ Test API với curl

```bash
# 1. Đăng ký
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"123456"}'

# 2. Đăng nhập → copy accessToken từ response
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 3. Lấy danh sách sản phẩm
curl http://localhost:8080/api/products

# 4. Tạo sản phẩm (thay YOUR_TOKEN bằng token thật)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name":"Samsung S24","category":"Điện thoại","brand":"Samsung","price":22990000}'
```

---

## 📮 Import vào Postman

1. Mở Postman → Click **Import**
2. Chọn **Raw text** và dán link:  
   `https://github.com/Tuong2608/sope-backend`
3. Hoặc tự tạo Collection với các endpoint ở trên
