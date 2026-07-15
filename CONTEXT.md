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
- Thay doi: brandContains tach brand query thanh cac term va match ca brand/name; Apple match iPhone/iPad/MacBook; response product suy luan brand tu name neu brand rong; DataSeeder doc first text trong mang long nhau de lan seed sau khong lam brand rong.
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
- Thay doi: simulateBankTransfer kiem tra payment thuoc user, chi nhan PENDING, tao transactionId SIM-*, cap nhat Payment SUCCESS va Order PAID; admin co API xem payment history theo status tuy chon.
- Kiem tra: ./mvnw.cmd -q -DskipTests compile pass.
- Luu y: GET /api/admin/stats tinh doanh thu tu order PAID/COMPLETED; neu user khong bam simulate thi payment van PENDING va doanh thu chua tang.

## 2026-07-12 - C01: So dia chi giao hang (Address book)
- Yeu cau: Chuan hoa dia chi nhan hang - luu nguoi nhan, SDT, tinh/huyen/xa, dia chi chi tiet; validate truong bat buoc.
- Package lien quan: entity, repository, dto/request, dto/response, service, controller.
- Da tao: entity/Address.java; repository/AddressRepository.java; dto/request/AddressRequest.java; dto/response/AddressResponse.java; service/AddressService.java; controller/AddressController.java.
- Endpoint: GET/POST /api/addresses; GET/PUT/DELETE /api/addresses/{id}; PUT /api/addresses/{id}/default. Tat ca yeu cau dang nhap (da duoc phu boi anyRequest().authenticated() co san, khong sua SecurityConfig).
- Thay doi: Address la so dia chi rieng cua User (1-n), moi field bat buoc (@NotBlank + regex SDT VN). Dia chi dau tien tu dong thanh default; AddressService dam bao chi 1 default/user; xoa default se promote dia chi con lai gan nhat.
- Kiem tra: ./mvnw.cmd -DskipTests compile pass; ./mvnw.cmd test (CartServiceTest, OrderServiceTest, PaymentServiceTest) 7/7 pass, khong bi anh huong.
- Luu y: Order.shippingAddress hien van la 1 string rieng (chua doi) - C06 (ngay 14) se noi Address vao luc tao don.
- Viec can lam tiep theo: C02-C05 (giao hang/ETA), C06 se dung Address entity nay de snapshot vao Order.

## 2026-07-12 - D01+D02: Chot quy tac va tao bang ma giam gia (Coupon)
- Yeu cau: Chot quy tac coupon (1 don 1 ma, giam % hoac so tien, ap dung SP/loai/toan don) va tao bang coupon + lich su giu/dung/tra luot.
- Package lien quan: entity, repository.
- Da tao: entity/DiscountType.java; entity/CouponScope.java; entity/CouponUsageStatus.java; entity/Coupon.java; entity/CouponUsage.java; repository/CouponRepository.java; repository/CouponUsageRepository.java.
- Endpoint: Chua co (D03 - API quan ly coupon - se lam ngay 13/07).
- Thay doi: Coupon luu code/discountType/discountValue/scope/applicableProductIds/applicableCategories/minOrderAmount/maxDiscountAmount/usageLimit/usageLimitPerUser/usedCount/startAt/endAt/active, kem helper isWithinValidPeriod()/hasReachedUsageLimit(). CouponUsage ghi 1 dong HELD/USED/RELEASED cho moi (coupon, order), unique(coupon_id, order_id) de enforce "1 don 1 ma" o DB.
- Kiem tra: ./mvnw.cmd -DskipTests compile pass (135 file). Chua co Controller/Service nghiep vu - chi entity + repository dung ddl-auto=update de tao bang.
- Luu y: discountValue dung BigDecimal (ap dung ca % lan VND tuy discountType); applicableProductIds/applicableCategories la @ElementCollection rieng, category la String giong Product.category (chua co entity Category rieng).
- Viec can lam tiep theo: D03 (API quan ly coupon cho Admin), D04 (ham tinh tien don), D05 (API thu/ap ma), D06 (giu/tra luot voi concurrency).

## 2026-07-13 - C02+C03+C04+C05: Khu vuc/phuong thuc giao hang, ngay nghi, tinh ETA, API xem truoc
- Yeu cau: Tao phuong thuc/khu vuc giao hang + phi + so ngay (C02); gio chot don + ngay nghi (C03); ham tinh ngay giao du kien + test bien (C04); API xem truoc ngay giao public (C05).
- Package lien quan: entity, repository, seeder, service, controller, dto/request, dto/response, config (SecurityConfig).
- Da tao: entity/ShippingZone.java, ShippingMethod.java, ShippingRate.java, Holiday.java; repository/ShippingZoneRepository.java, ShippingMethodRepository.java, ShippingRateRepository.java, HolidayRepository.java; seeder/ShippingDataSeeder.java, HolidayDataSeeder.java (ApplicationRunner, @Profile("!test"), giong pattern LaptopDataSeeder); service/DeliveryEstimateService.java; controller/DeliveryController.java; dto/request/DeliveryEstimateRequest.java, DeliveryEstimateItemRequest.java; dto/response/DeliveryEstimateResponse.java; test/service/DeliveryEstimateServiceTest.java.
- Da sua: config/SecurityConfig.java (them permitAll cho POST /api/delivery/estimate).
- Endpoint: POST /api/delivery/estimate (public) - nhan {province, methodCode, items:[{productId,quantity}]}, tra {zoneName, methodCode, fee, estimatedMinDate, estimatedMaxDate, note}.
- Thay doi: ShippingZone match theo danh sach tinh/thanh (uu tien theo field priority khi 1 tinh thuoc nhieu zone); ShippingRate = phi+minDays/maxDays cho tung cap (zone, method); DeliveryEstimateService: dat sau gio chot don (app.shipping.cutoff-hour, mac dinh 18h) -> +1 ngay xu ly; item thieu ton kho (Product.getAvailableQuantity()) -> cong them app.shipping.restock-delay-days (mac dinh 3); ngay giao roi vao Holiday -> doi sang ngay ke tiep.
- Kiem tra: ./mvnw.cmd -DskipTests compile pass. DeliveryEstimateServiceTest 6/6 pass (truoc/sau gio chot don, ngay giao trung ngay nghi, het hang cong ngay, tinh khong thuoc zone nao, method khong co rate o zone). Da doi chieu: 2 loi CartServiceTest + EcommerceBackendApplicationTests.contextLoads la loi CO SAN tu truoc (tu B05 kiem tra ton kho gio hang + thieu cau hinh DB test cho @SpringBootTest), khong lien quan code hom nay - da verify bang git stash roi chay lai tren baseline truoc khi code.
- Luu y: @Value co default (cutoff-hour:18, restock-delay-days:3) de khong phu thuoc application.properties (da bi gitignore, may dev khong co san). Chua co API quan tri Holiday/ShippingZone/Rate rieng - chi seed san du lieu mau, admin sua truc tiep qua DB neu can trong phase nay.
- Viec can lam tiep theo: C06 (luu shipping method/phi/ETA vao Order, dung Address), C07 (chuyen trang thai don).

## 2026-07-13 - D03: API quan ly ma giam gia cho Admin
- Yeu cau: Admin them/sua/tat ma; kiem tra thoi gian, so luot, don toi thieu va muc giam toi da.
- Package lien quan: dto/request, dto/response, service, controller/admin.
- Da tao: dto/request/CouponRequest.java; dto/response/CouponResponse.java; service/CouponService.java; controller/admin/AdminCouponController.java.
- Endpoint (/api/admin/coupons, ROLE_ADMIN - da duoc SecurityConfig phu san qua /api/admin/**, khong sua gi them): GET (loc ?active=), GET /{id}, POST, PUT /{id}, PUT /{id}/activate, PUT /{id}/deactivate.
- Thay doi: CouponService validate discountValue PERCENTAGE <=100, scope=SPECIFIC_PRODUCTS/CATEGORIES phai co applicableProductIds/applicableCategories tuong ung, startAt<endAt neu ca 2 co gia tri, code duy nhat (uppercase hoa truoc khi luu/so sanh). "Tat ma" = toggle active=false, khong xoa cung (giu lich su CouponUsage).
- Kiem tra: ./mvnw.cmd -DskipTests compile pass.
- Viec can lam tiep theo: D04 (ham tinh tien don ap dung coupon), D05 (API thu/ap ma - doc lai gio hang, khong nhan tong tien tu frontend), D06 (giu/tra luot voi concurrency).

## 2026-07-14 - D04+D05+C06+C07: Tinh tien don, thu/ap ma, luu shipping vao don, chuyen trang thai don
- Yeu cau: Ham tinh tam tinh/giam gia/phi giao/tong cuoi + chia giam gia xuong tung SP (D04); API thu/ap ma doc lai gio/gia/ton, khong nhan tong tien tu FE (D05); luu phuong thuc/phi/ETA giao hang vao don (C06); chi cho chuyen trang thai dung thu tu (C07).
- Package lien quan: entity, dto/request, dto/response, service, controller.
- Da tao: service/OrderPricingService.java (D04 core - dung chung cho D05 va C06), service/OrderPricingResult.java (ket qua noi bo, khong phai HTTP DTO); service/CouponPreviewService.java + controller/CouponPreviewController.java + dto ApplyCouponPreviewRequest/CouponPreviewResponse/CouponPreviewItemResponse (D05); test/service/OrderPricingServiceTest.java (7 case: khong ma, % ALL_ORDER, SPECIFIC_PRODUCTS chi giam SP thuoc pham vi, cap maxDiscountAmount, duoi minOrderAmount, het han, het luot).
- Da sua: entity/OrderStatus.java (them PROCESSING, sap xep PENDING->PAID->PROCESSING->SHIPPING->COMPLETED, CANCELLED tu PENDING/PAID/PROCESSING); entity/Order.java (them subtotalAmount, discountAmount, couponCode, shippingMethodCode, shippingFee, estimatedDeliveryMinDate/MaxDate); entity/OrderItem.java (them discountAmount - phan bo giam gia tung dong); dto/request/CreateOrderRequest.java (them province bat buoc, shippingMethodCode, couponCode - GIU NGUYEN cac field cu, khong breaking); dto/response/OrderResponse.java + OrderItemResponse.java (them field tuong ung); service/OrderService.java (createOrder goi OrderPricingService + DeliveryEstimateService; updateStatus them state machine C07 qua ALLOWED_TRANSITIONS map + tu dong tru/hoan kho + dong bo CouponUsage HELD->USED/RELEASED); test/service/OrderServiceTest.java (cap nhat mock cho 4 dependency moi + them 5 test C07: PENDING->PAID tru kho, PAID->CANCELLED hoan kho, PENDING->CANCELLED KHONG dung toi kho, chan nhay buoc PENDING->SHIPPING, chan chuyen tu trang thai terminal COMPLETED).
- Endpoint: POST /api/coupons/apply-preview (auth, D05). POST /api/orders (C06, da co tu tuan 2, gio tra them subtotalAmount/discountAmount/couponCode/shippingFee/estimatedDeliveryMinDate/MaxDate). PUT /api/admin/orders/{id}/status (C07, da co tu D03 nhung gio moi validate thu tu chuyen trang thai).
- Bug fix phat hien trong pham vi: OrderService.cancelOrder (chi cho huy khi PENDING) truoc day van goi inventoryService.restoreStockForOrder du PENDING chua tung bi tru kho -> cong du ton kho. Da bo cuoc goi thua nay (PENDING khong dung toi inventory); logic hoan kho dung dan (chi khi tu PAID/PROCESSING) chuyen sang updateStatus.
- Kiem tra: ./mvnw.cmd -DskipTests compile pass. OrderPricingServiceTest 7/7, OrderServiceTest 7/7 (bao gom 5 test C07 moi), DeliveryEstimateServiceTest 6/6, PaymentServiceTest 3/3, ProductRepositoryIntegrationTest 4/4 - tat ca pass. CartServiceTest + EcommerceBackendApplicationTests van loi (pre-existing tu truoc, da xac nhan khong lien quan qua git stash o PR ngay 13).
- Luu y: CouponUsage duoc tao HELD ngay luc dat don (createOrder) neu co coupon hop le; markAsPaid/updateStatus(PAID) chuyen HELD->USED + Coupon.usedCount++; cancelOrder/updateStatus(CANCELLED) chuyen HELD->RELEASED. Day la nen tang co ban (chua co optimistic locking/concurrency that su) - D06 (ngay 15) se hoan thien chong race-condition khi nhieu nguoi dung cung mot ma cung luc.
- Viec can lam tiep theo: D06 (khoa concurrency that su cho giu/tra luot coupon), C10 (dich vu van chuyen gia lap sinh ma van don), H03 (trang quan ly don nang cao - tim/loc/ETA/ma van don).

## 2026-07-15 - H02+H09: Dashboard nang cao + Quan ly ton kho thu cong

- Yeu cau: H02 - thong ke doanh thu hom nay/7/30 ngay, % tang truong, top SP ban chay; H09 - admin nhap hang, set stock, doi trang thai, cap nhat minStockLevel, bulk restock.
- Package lien quan: repository, service, controller/admin.
- Da tao: service/DashboardService.java (H02: DashboardOverview, PeriodRevenueStats, TopProductStats; tinh revenue/don theo ky, so sanh voi ky truoc); controller/admin/AdminDashboardController.java (/api/admin/dashboard/overview, /revenue, /top-products); service/AdminInventoryManagementService.java (H09: restockProduct, restockVariant, setStock, updateStatus, updateMinStockLevel, bulkRestock + auto chuyen ACTIVE/OUT_OF_STOCK); controller/admin/AdminInventoryManagementController.java (POST /restock/{id}, POST /restock/variant/{id}, POST /bulk-restock, PUT /set-stock/{id}, PUT /status/{id}, PUT /min-stock/{id}).
- Da sua: repository/OrderRepository.java (them revenueByPeriod, countByStatusAndPeriod, countByPeriod, findTopSellingProducts voi @Param from/to/limit/status).
- Endpoint (deu can ROLE_ADMIN): GET /api/admin/dashboard/overview; GET /api/admin/dashboard/revenue?from=&to=; GET /api/admin/dashboard/top-products?from=&to=&limit=10; POST /api/admin/inventory/restock/{productId}; POST /api/admin/inventory/restock/variant/{variantId}; POST /api/admin/inventory/bulk-restock; PUT /api/admin/inventory/set-stock/{productId}; PUT /api/admin/inventory/status/{productId}; PUT /api/admin/inventory/min-stock/{productId}.
- Kiem tra: ./mvnw.cmd -DskipTests compile pass (BUILD SUCCESS).
- Viec can lam tiep theo: D06 concurrency coupon, C10 ma van don, H03 quan ly don nang cao.

## 2026-07-15 - D06+C10+H03: Khoa concurrency coupon, van chuyen gia lap, quan ly don nang cao
- Yeu cau: D06 - giu luot khi dat don, dung khi thanh toan, tra khi huy, kiem tra khi nhieu nguoi dung cung luc; C10 - dich vu van chuyen gia lap sinh ma don + cap nhat trang thai; H03 - admin tim/loc/xem chi tiet/cap nhat trang thai/xem ETA+ma van don.
- Package lien quan: repository, service, entity, dto/response, controller, controller/admin, specification.
- Da tao: entity/ShipmentStatus.java, ShipmentTracking.java, ShipmentTrackingEvent.java; repository/ShipmentTrackingRepository.java, ShipmentTrackingEventRepository.java; service/ShipmentTrackingService.java (createForOrder idempotent, advance() theo thu tu CREATED->PICKED_UP->IN_TRANSIT->OUT_FOR_DELIVERY->DELIVERED); dto/response/ShipmentTrackingResponse.java, ShipmentTrackingEventResponse.java; controller/ShipmentTrackingController.java (user, GET /api/orders/{id}/tracking), controller/admin/AdminShipmentTrackingController.java (GET + PUT .../tracking/advance); specification/OrderSpecifications.java (keywordContains orderCode/recipientName/phone, statusEquals, createdBetween); test/service/ShipmentTrackingServiceTest.java (5 case).
- Da sua: repository/CouponRepository.java (them findByCodeForUpdate voi @Lock(PESSIMISTIC_WRITE) - D06); repository/CouponUsageRepository.java (them countByCouponIdAndStatus); repository/OrderRepository.java (them extends JpaSpecificationExecutor<Order>); service/OrderPricingService.java (them overload price(cart, couponCode, user, holdForCheckout) - khi true thi khoa coupon row + tinh ca luot dang HELD vao usageLimit check, chu ky cu 3 tham so van giu nguyen cho D05 preview khong khoa); service/OrderService.java (createOrder goi price(..., true); updateStatus khi SHIPPING thi tao shipment tracking; them searchOrders(keyword, status, from, to, pageable) thay the getAllOrders); dto/response/OrderResponse.java (them trackingNumber, trackingStatus); controller/admin/AdminOrderController.java (endpoint GET doi sang PagedResponse<OrderResponse>, them keyword/from/to/page/size).
- Endpoint moi: GET/PUT /api/orders/{id}/tracking (user), GET/PUT /api/admin/orders/{id}/tracking(/advance) (admin). Endpoint thay doi shape: GET /api/admin/orders gio tra PagedResponse<OrderResponse> (truoc la List<OrderResponse>) - **frontend (Nguyen) can biet de cap nhat UI trang quan ly don** neu da tich hop.
- Kiem tra: ./mvnw.cmd -DskipTests compile pass. OrderPricingServiceTest 9/9 (them 2 case D06 concurrency: held-count chan khi holdForCheckout=true, preview khong bi anh huong), OrderServiceTest 8/8 (them case SHIPPING tao tracking), ShipmentTrackingServiceTest 5/5, DeliveryEstimateServiceTest, PaymentServiceTest, ProductRepositoryIntegrationTest deu pass. CartServiceTest + EcommerceBackendApplicationTests + SecurityAccessIntegrationTest (test moi cua nhom, dung @SpringBootTest can MySQL that) loi khi chay dong thoi do tranh chap ket noi DB - da xac minh bang git stash + chay rieng le tung suite deu PASS ca tren baseline lan voi code hom nay, khong lien quan logic code.
- Luu y: D06 dung pessimistic lock (SELECT...FOR UPDATE) tren Coupon row trong transaction tao don - 2 checkout dong thoi voi cung coupon se tuan tu hoa, khong the cung vuot usageLimit. C10 la carrier gia lap hoan toan (khong goi API van chuyen that), trang thai chi tien (khong lui), DELIVERED la cuoi. searchOrders/OrderSpecifications theo dung pattern ProductSpecifications/ProductService.search da co san.
- Day la ngay cuoi cung trong ke hoach 12-15/07 cua Toan (C01-C10, D01-D06). Tat ca task duoc giao da hoan thanh.
