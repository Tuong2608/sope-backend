# CONTEXT.md - Bộ nhớ riêng cho sope-backend

## 1. Vai trò của backend

`sope-backend/` là phần backend Java Spring Boot của dự án SOPE.

Backend phụ trách:

- Cung cấp REST API cho frontend.
- Cung cấp dữ liệu/API cho chatbot nếu cần.
- Xác thực, phân quyền, bảo mật.
- Xử lý nghiệp vụ thương mại điện tử.
- Quản lý database.
- Quản lý entity, repository, service, controller, DTO, exception.
- Xử lý tìm kiếm/lọc động qua specification nếu có.
- Seed dữ liệu ban đầu nếu có.

---

## 2. Cấu trúc backend hiện tại

```text
sope-backend/
├─ AGENTS.md
├─ CONTEXT.md
└─ src/
   └─ main/
      ├─ java/
      │  └─ com/
      │     └─ ecommerce/
      │        └─ ecommercebackend/
      │           ├─ config/
      │           ├─ controller/
      │           ├─ dto/
      │           ├─ entity/
      │           ├─ exception/
      │           ├─ repository/
      │           ├─ security/
      │           ├─ seeder/
      │           ├─ service/
      │           ├─ specification/
      │           ├─ util/
      │           └─ EcommerceBackendApplication.java
      └─ resources/
```

---

## 3. Công nghệ backend

- Ngôn ngữ: Java.
- Framework: Spring Boot.
- Package gốc: `com.ecommerce.ecommercebackend`.
- Main class: `EcommerceBackendApplication.java`.
- Database: Cần cập nhật.
- ORM: Spring Data JPA/Hibernate nếu dự án đang dùng.
- Security: Spring Security/JWT nếu dự án đang dùng.
- Build tool: Cần xác định Maven hoặc Gradle.

Cần cập nhật thêm:

- Java version:
- Spring Boot version:
- Database:
- Maven hay Gradle:
- Lệnh chạy:
- Lệnh test:
- File cấu hình chính trong `resources/`:

---

## 4. Ý nghĩa từng package

### `config/`

Dùng cho cấu hình:

- CORS.
- Bean chung.
- Swagger/OpenAPI nếu có.
- Cấu hình phụ trợ.

Cần tránh hard-code secret, password, API key.

### `controller/`

Dùng cho REST API endpoint.

Controller nên:

- Nhận request.
- Validate cơ bản.
- Gọi service.
- Trả response.

Không nên chứa business logic dài.

### `dto/`

Dùng cho request/response object.

DTO giúp:

- Không trả trực tiếp entity nếu dự án đã dùng DTO.
- Không lộ field nhạy cảm.
- Chuẩn hóa dữ liệu gửi/nhận với frontend/chatbot.

### `entity/`

Dùng cho JPA entity/database model.

Cần cẩn thận khi sửa:

- Tên bảng.
- Tên cột.
- Kiểu dữ liệu.
- Quan hệ entity.
- Serialize JSON khi có quan hệ hai chiều.
- Field nhạy cảm như password.

### `exception/`

Dùng cho custom exception và global exception handling.

Cần đảm bảo:

- Không trả stack trace cho client.
- Message lỗi rõ nhưng không lộ dữ liệu nhạy cảm.
- Lỗi validate trả về phù hợp.

### `repository/`

Dùng cho Spring Data JPA repository.

Repository chỉ nên xử lý truy vấn database, không chứa business logic.

### `security/`

Dùng cho:

- Spring Security config.
- JWT/filter.
- UserDetails nếu có.
- Password encoder.
- Auth middleware.
- Role/permission.

Không được bỏ security để sửa lỗi nhanh.

### `seeder/`

Dùng cho dữ liệu khởi tạo.

Cần tránh seed trùng hoặc xóa dữ liệu thật.

### `service/`

Dùng cho business logic.

Service nên là nơi xử lý nghiệp vụ chính.

### `specification/`

Dùng cho tìm kiếm/lọc động.

Cần kiểm tra null/empty filter để tránh lỗi.

### `util/`

Dùng cho helper dùng chung.

Không đưa business logic riêng hoặc logic bảo mật quan trọng vào util nếu đã có package phù hợp.

---

## 5. Endpoint quan trọng

| Chức năng | Method | Endpoint | Controller | Service | Frontend/chatbot dùng | Ghi chú |
|---|---|---|---|---|---|---|
| Login | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Frontend | Cần điền |
| Register | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Frontend | Cần điền |
| Product list | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Frontend/Chatbot | Cần điền |
| Product detail | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Frontend/Chatbot | Cần điền |
| Cart | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Frontend | Cần điền |
| Order/checkout | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Frontend | Cần điền |
| Admin | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Frontend | Cần điền |

---

## 6. Entity/database quan trọng

| Entity | File | Field chính | Quan hệ | Ghi chú |
|---|---|---|---|---|
| User | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Cần điền |
| Product | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Cần điền |
| Category | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Cần điền |
| Cart | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Cần điền |
| Order | Chưa cập nhật | Chưa cập nhật | Chưa cập nhật | Cần điền |

---

## 7. Auth và security

Cần cập nhật:

- Cơ chế đăng nhập:
- Token/JWT:
- Nơi cấu hình security:
- Password encoder:
- Role hiện có:
- Endpoint public:
- Endpoint yêu cầu user:
- Endpoint yêu cầu admin:
- CORS config:
- Biến môi trường liên quan:

Quy tắc:

- Không lưu password plain text.
- Không log token/password/secret.
- Không đưa secret vào code.
- Không trả field nhạy cảm qua DTO.
- Không mở public API admin.

---

## 8. API phục vụ chatbot

Nếu chatbot lấy dữ liệu từ backend, cần ghi rõ:

| Nhu cầu chatbot | Endpoint backend | Dữ liệu trả về | Ghi chú |
|---|---|---|---|
| Recommend sản phẩm | Chưa cập nhật | Chưa cập nhật | Cần điền |
| Tìm sản phẩm | Chưa cập nhật | Chưa cập nhật | Cần điền |
| Chi tiết sản phẩm | Chưa cập nhật | Chưa cập nhật | Cần điền |

---

## 9. Lỗi backend cũ cần tránh

| Ngày | Lỗi | Nguyên nhân | Cách tránh | Package/file liên quan |
|---|---|---|---|---|
| Chưa có | Chưa có | Chưa có | Chưa có | Chưa có |

---

## 10. Nhật ký làm việc backend gần nhất

### Lần 1

- Ngày:
- Người dùng yêu cầu:
- Package liên quan:
- File đã sửa:
- Endpoint liên quan:
- Entity/database liên quan:
- Nội dung thay đổi:
- Có ảnh hưởng frontend không:
- Có ảnh hưởng chatbot không:
- Lỗi gặp phải:
- Cách xử lý:
- Cách kiểm tra API:
- Có cập nhật root CONTEXT.md không:
- Việc cần làm tiếp theo:

---

## 11. Cách kiểm tra backend

Nếu dùng Maven Wrapper:

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

Nếu dùng Maven cài sẵn:

```bash
mvn spring-boot:run
mvn test
mvn clean package
```

Nếu dùng Gradle:

```bash
./gradlew bootRun
./gradlew test
./gradlew build
```

Khi kiểm tra API cần ghi:

- Endpoint:
- Method:
- Headers:
- Body mẫu:
- Kết quả mong đợi:
- Frontend/chatbot có bị ảnh hưởng không:

---

## 12. Việc cần làm tiếp theo cho backend

- [ ] Xác định Maven hay Gradle.
- [ ] Điền database đang dùng.
- [ ] Ghi lại các controller chính.
- [ ] Ghi lại các entity chính.
- [ ] Ghi lại API auth.
- [ ] Ghi lại API product.
- [ ] Ghi lại API phục vụ chatbot nếu có.
- [ ] Ghi lại lỗi backend nếu phát sinh.
