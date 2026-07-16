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

## 2026-07-08 - Product popularity sort by rating
- Yeu cau: Sap xep do pho bien cua san pham theo rating_stars cao den thap.
- Da sua: src/main/java/com/ecommerce/ecommercebackend/controller/ProductController.java; src/main/java/com/ecommerce/ecommercebackend/service/ProductService.java.
- Thay doi: ProductController whitelist ratingStars va chap nhan rating_stars; ProductService phat hien sort ratingStars, lay danh sach da filter, tinh trung binh reviews.ratingStars, sort roi tao PageImpl de giu response phan trang.
- Kiem tra: ./mvnw.cmd -q -DskipTests compile pass.
- Luu y: Cach nay phu hop tap du lieu hien tai; neu du lieu rat lon can toi uu bang query aggregate trong repository.

## 2026-07-08 - Fix brand filter fallback
- Yeu cau: Loc hang iPhone/iPad khong ra san pham do DB hien tai brand rong, trong data goc brand la mang long nhau nhu iPhone (Apple), iPad (Apple).
- Da sua: src/main/java/com/ecommerce/ecommercebackend/specification/ProductSpecifications.java; src/main/java/com/ecommerce/ecommercebackend/service/ProductService.java; src/main/java/com/ecommerce/ecommercebackend/seeder/DataSeeder.java.
- Thay doi: brandContains tach brand query thanh cac term va match ca brand/name; Apple match iPhone/iPad/MacBook; response product suy luan brand tu name neu brand rong; DataSeeder doc first text### Ngày 14/07/2026: Idempotency, Rate Limiting & Admin Security
- **Yêu cầu xử lý:** Chống tạo đơn hai lần (Idempotency), Giới hạn API (Rate Limiting), Bảo vệ phòng chat WebSocket, Test phân quyền, Ngăn Admin tự khóa/tự giáng quyền.
- **File thay đổi chính:**
  - `OrderController.java`: Bổ sung ConcurrentHashMap cache cho Idempotency-Key.
  - `RateLimitFilter.java` & `pom.xml`: Thêm thư viện `bucket4j-core` và config Filter.
  - `WebSocketAuthInterceptor.java`: Chặn client gửi lệnh SUBSCRIBE sai user.
  - `AdminUserController.java` & `AdminService.java`: Truyền current user và ném lỗi `BadRequestException` nếu tự khoá hoặc giáng cấp admin cuối cùng.
  - `SecurityAccessIntegrationTest.java`: Viết Test JUnit xác minh quyền truy cập bằng TestRestTemplate & Unit Test Services.
- **Thử nghiệm:** Chạy `./mvnw clean test` thành công (sau khi fix lỗi liên quan đến application-test.properties và MockMvc AutoConfigure).
- **Trạng thái:** Hoàn tất 5 commits. Code an toàn.
- Kiem tra: ./mvnw.cmd -q -DskipTests compile pass.
- Luu y: DB hien co khong can reset; neu reset/seed lai thi brand se duoc import dung hon.

## 2026-07-08 - Forgot password backend
- Yeu cau: Ho tro quen mat khau/dat lai mat khau cho auth.
- Da sua: controller/AuthController.java; service/AuthService.java; entity/User.java; repository/UserRepository.java; dto/request/ForgotPasswordRequest.java; dto/request/ResetPasswordRequest.java; dto/response/PasswordResetResponse.java; src/main/resources/application.properties.
- Endpoint: POST /api/auth/forgot-password; POST /api/auth/reset-password.
- Thay doi: Tao token reset bang SecureRandom, luu SHA-256 hash va thoi han 30 phut tren users; reset mat khau se validate token, ma hoa password moi va xoa token. Them config app.frontend.base-url va app.password-reset.expiration-minutes.
- Kiem tra: ./mvnw.cmd -q -DskipTests compile pass.
- Luu y: Can restart backend de ddl-auto=update them cot moi; chua cau hinh SMTP nen response co resetLink de frontend test local.

## 2026-07-08 - Payment demo va lich su thanh toan admin
- Yeu cau: Gia lap buoc chuyen khoan cho thanh toan VNPAY/MoMo va luu lich su thanh toan de admin thong ke doanh thu.
- Package lien quan: controller, controller/admin, service, repository, entity payment/order.
- Da sua: controller/PaymentController.java; controller/admin/AdminPaymentController.java; service/PaymentService.java; repository/PaymentRepository.java.
- Endpoint: POST /api/payment/create; POST /api/payment/{id}/simulate-bank-transfer; GET /api/admin/payments; GET /api/admin/stats.
- Thay doi: simulateBankTransfer kiem tra payment thuoc user, chi nhan PENDING, tao tao transactionId SIM-*, cap nhat Payment SUCCESS va Order PAID; admin co API xem payment history theo status tuy chon.
- Kiem tra: ./mvnw.cmd -q -DskipTests compile pass.
- Luu y: GET /api/admin/stats tinh doanh thu tu order PAID/COMPLETED; neu user khong bam simulate thi payment van PENDING va doanh thu chua tang.
## 2026-07-13 - Bảo mật luồng đăng nhập, API chat/rating, CORS
- Yêu cầu: G03 Đăng nhập cookie HttpOnly, G04 Gửi link quên mật khẩu giả lập, G06 Bảo vệ API chat/rating khỏi giả mạo, G09 Cấu hình CORS/Security Headers.
- Đã sửa: `AuthController.java`, `JwtAuthenticationFilter.java`, `AuthService.java`, `PasswordResetResponse.java`, `ChatController.java`, `ReviewController.java`, `SecurityConfig.java`.
- Thay đổi: 
  - G03: Bổ sung cookie HttpOnly cho `/login` và `/google`, `JwtAuthenticationFilter` hỗ trợ đọc token từ cookie, thêm `/logout` để xoá cookie.
  - G04: Không trả `resetLink` và `expiresAt` trong HTTP response, giả lập việc gửi email link đổi mật khẩu (in ra console).
  - G06: Bổ sung `@AuthenticationPrincipal` kiểm tra quyền cho `getHistory` trong `ChatController` và check X-Chatbot-Secret cho `save`. Review Controller đã an toàn nhờ kiểm tra user trong service.
  - G09: Thêm `allowCredentials(true)` trong CORS và bổ sung Security Headers (frame options, xss, csp).
- Kiểm tra: Maven build pass (`./mvnw clean package -DskipTests`).
- Lưu ý: Frontend cần chú ý sử dụng config `withCredentials: true` do đã chuyển sang xác thực bằng HttpOnly Cookie.

## 2026-07-16 - Catalog, C08/H06, cart và chatbot proxy

- Yêu cầu: tích hợp Backend với MySQL thật, Frontend và Chatbot; hoàn thiện API cart, shipping, health và production config.
- Đã sửa: `DataSeeder`, cart DTO/repository/service/test, security/CORS, RestTemplate timeout, exception handler, chat/recommendation controllers; thêm health controller, admin shipping controller/service và DTO chat; cập nhật test config, env example và application local.
- Catalog: seeder idempotent theo SKU, chạy trước laptop mẫu, chỉ thêm bản ghi thiếu; đã bổ sung 266 sản phẩm mà không xóa/ghi đè dữ liệu hiện có.
- Cart: entity graph tải items/product/variant; response có `variantId`, `colorName`, `storageName`, `availableQuantity`, `inStock`; validate variant thuộc product, active và đủ tồn kho.
- Chat: POST `/api/chat` proxy FastAPI; URL/timeout từ env; `ResponseStatusException` giữ đúng HTTP status; `/api/health` kiểm tra `SELECT 1`.
- H06: GET methods/zones/rates và PATCH trạng thái, chỉ ROLE_ADMIN; frontend dùng trực tiếp các endpoint này.
- Kiểm tra: Maven test/package pass; runtime health/database UP; delivery estimate UTF-8 pass; catalog 162 phone/54 tablet/60 laptop; search iPhone 12 kết quả; chat proxy trả dữ liệu catalog thật.
- Việc tiếp theo: cần credential admin hiện tại để smoke test runtime có JWT cho cart/H06; không tự đồng bộ/ghi đè mật khẩu admin trong DB.

## 2026-07-17 - Nâng cấp Payment Sandbox

- Yêu cầu: bỏ payment mô phỏng, hoàn thiện COD/VNPAY/MoMo Sandbox, chữ ký, callback/IPN, idempotency, retry và dữ liệu kết quả thật.
- Package: config, controller, dto, entity, repository, service, resources migration và test.
- Endpoint: giữ `POST /api/payment/create`, `GET /api/payment/{id}`; thêm `POST /api/payment/{id}/retry`; callback/IPN VNPAY và MoMo public đúng phạm vi; xóa `simulate-bank-transfer`.
- Contract: request chỉ nhận orderId/provider/channel; amount/orderInfo lấy từ Order. Response có order/payment/provider metadata, signatureVerified, canRetry, payUrl/deeplink/QR URL thật.
- Nghiệp vụ: IPN khóa Payment/Order và gọi `OrderService.markAsPaid`; tồn kho/coupon chỉ xử lý ở lần chuyển PENDING->PAID. Return không hoàn tất giao dịch.
- Database: Flyway baseline 0, migration V1/V2 cho payments; test H2 tắt Flyway.
- Bảo mật: payment/DB/JWT/admin secret đọc environment; CORS dùng APP_FRONTEND_ORIGINS; không log secret/raw signature.
- Kiểm tra cuối: `mvnw.cmd clean test` qua 10 suite/42 test, không failure/error; `mvnw.cmd package -DskipTests` pass. Chưa test provider thật do thiếu credential/Ngrok.
- Tiếp theo: điền env ngoài Git, chạy Ngrok và smoke test theo `../PAYMENT_SETUP_GUIDE.md`.
