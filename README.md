# 🛍️ SOPE Backend

Backend RESTful API cho dự án SOPE, xây dựng bằng **Spring Boot 4**, **MySQL** và **JWT Authentication**.

---

## 📋 Yêu cầu cài đặt

Trước khi chạy project, hãy đảm bảo máy bạn đã cài đầy đủ:

| Công cụ | Phiên bản tối thiểu | Link tải |
|--------|-------------------|----------|
| **JDK** | 17 trở lên | https://www.oracle.com/java/technologies/downloads/#java17 |
| **Maven** | 3.8+ (hoặc dùng `mvnw` có sẵn) | https://maven.apache.org/download.cgi |
| **MySQL** | 8.0+ | https://dev.mysql.com/downloads/mysql/ |
| **Git** | Mới nhất | https://git-scm.com/downloads |

> 💡 **Tip**: Kiểm tra phiên bản đã cài bằng lệnh:
> ```bash
> java -version
> mvn -version
> mysql --version
> ```

---

## 🚀 Hướng dẫn chạy project

### Bước 1: Clone repository

```bash
git clone git@github.com:Tuong2608/sope-backend.git
cd sope-backend
```

> Nếu chưa có SSH key, dùng HTTPS:
> ```bash
> git clone https://github.com/Tuong2608/sope-backend.git
> ```

---

### Bước 2: Cấu hình Database

Mở MySQL và tạo database (hoặc để Spring tự tạo):

```sql
CREATE DATABASE IF NOT EXISTS ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Mở file cấu hình:

```
src/main/resources/application.properties
```

Chỉnh lại thông tin kết nối theo máy của bạn:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root        # ← đổi thành username MySQL của bạn
spring.datasource.password=root        # ← đổi thành password MySQL của bạn
```

> ⚠️ **Lưu ý**: Không commit file `application.properties` sau khi đã chỉnh thông tin cá nhân lên GitHub!

---

### Bước 3: Chạy Backend

**Cách 1 — Dùng Maven Wrapper (khuyên dùng, không cần cài Maven):**

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

**Cách 2 — Dùng Maven đã cài sẵn:**

```bash
mvn spring-boot:run
```

**Cách 3 — Build JAR rồi chạy:**

```bash
mvn clean package -DskipTests
java -jar target/sope-backend-0.0.1-SNAPSHOT.jar
```

✅ Server khởi động thành công khi thấy log:

```
Started EcommerceBackendApplication in X.XXX seconds
```

**Base URL:** `http://localhost:8080`

---

## 📡 Danh sách API

### 🔐 Authentication

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `POST` | `/api/auth/register` | Đăng ký tài khoản mới | ❌ Không cần |
| `POST` | `/api/auth/login` | Đăng nhập, nhận JWT token | ❌ Không cần |

#### Đăng ký tài khoản
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123"
}
```

#### Đăng nhập
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

> **Response trả về:**
> ```json
> {
>   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
>   "tokenType": "Bearer"
> }
> ```

---

### 📦 Products

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/products` | Lấy danh sách sản phẩm (có filter, phân trang) | ❌ Không cần |
| `GET` | `/api/products/{id}` | Lấy sản phẩm theo ID | ❌ Không cần |
| `POST` | `/api/products` | Tạo sản phẩm mới | ✅ Cần JWT |
| `PUT` | `/api/products/{id}` | Cập nhật sản phẩm | ✅ Cần JWT |
| `DELETE` | `/api/products/{id}` | Xóa sản phẩm | ✅ Cần JWT |

#### Lấy danh sách sản phẩm (có filter)

```http
GET http://localhost:8080/api/products?keyword=iphone&category=Điện thoại&brand=Apple&minPrice=10000000&maxPrice=50000000&page=0&size=10&sortBy=price&sortDir=asc
```

**Query Parameters:**

| Tham số | Mô tả | Mặc định |
|---------|-------|----------|
| `keyword` | Tìm kiếm theo tên sản phẩm | - |
| `category` | Lọc theo danh mục | - |
| `brand` | Lọc theo thương hiệu | - |
| `minPrice` | Giá tối thiểu (VND) | - |
| `maxPrice` | Giá tối đa (VND) | - |
| `page` | Số trang (bắt đầu từ 0) | `0` |
| `size` | Số sản phẩm mỗi trang (tối đa 100) | `10` |
| `sortBy` | Sắp xếp theo: `id`, `name`, `price`, `oldPrice`, `category`, `brand` | `id` |
| `sortDir` | Chiều sắp xếp: `asc` hoặc `desc` | `asc` |

#### Tạo sản phẩm mới

```http
POST http://localhost:8080/api/products
Authorization: Bearer <your_jwt_token>
Content-Type: application/json

{
  "name": "iPhone 15 Pro Max",
  "category": "Điện thoại",
  "brand": "Apple",
  "price": 34990000,
  "oldPrice": 37990000,
  "description": "iPhone 15 Pro Max 256GB"
}
```

---

## 🔑 Cách dùng JWT Token

Sau khi đăng nhập thành công, thêm token vào header của các request cần xác thực:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Trong Postman:**
1. Chọn tab **Authorization**
2. Chọn Type: **Bearer Token**
3. Dán token vào ô **Token**

---

## 🛠️ Công cụ test API gợi ý

- **Postman**: https://www.postman.com/downloads/
- **Thunder Client** (VS Code Extension): tìm trong Extensions của VS Code
- **curl** (command line):

```bash
# Test đăng nhập
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"password123"}'
```

---

## 📁 Cấu trúc thư mục

```
sope-backend/
├── src/
│   └── main/
│       ├── java/com/ecommerce/ecommercebackend/
│       │   ├── config/          # Cấu hình Security
│       │   ├── controller/      # REST Controllers (API endpoints)
│       │   ├── dto/             # Request & Response DTOs
│       │   ├── entity/          # JPA Entities (bảng database)
│       │   ├── exception/       # Xử lý lỗi toàn cục
│       │   ├── repository/      # Spring Data JPA Repositories
│       │   ├── security/        # JWT Filter, UserDetailsService
│       │   ├── service/         # Business Logic
│       │   ├── specification/   # JPA Specifications (query filter)
│       │   └── util/            # Các tiện ích
│       └── resources/
│           └── application.properties  # Cấu hình ứng dụng
├── pom.xml                      # Maven dependencies
└── README.md
```

---

## ❗ Lỗi thường gặp

### ❌ `Access denied for user 'root'@'localhost'`
→ Sai username/password MySQL. Kiểm tra lại `application.properties`.

### ❌ `Communications link failure`
→ MySQL chưa được khởi động. Mở MySQL Service hoặc chạy MySQL Workbench.

### ❌ `Port 8080 already in use`
→ Đã có chương trình khác chiếm port 8080. Thêm vào `application.properties`:
```properties
server.port=8081
```

### ❌ `java: error: release version 17 not supported`
→ JDK cài chưa đúng phiên bản. Cần JDK 17+.

---

## 🤝 Quy trình làm việc nhóm

```bash
# Trước khi bắt đầu làm việc, luôn pull code mới nhất
git pull origin main

# Sau khi làm xong, commit và push
git add .
git commit -m "feat: mô tả thay đổi của bạn"
git push origin main
```

---

*Nếu gặp vấn đề, liên hệ team lead hoặc tạo Issue trên GitHub.*
