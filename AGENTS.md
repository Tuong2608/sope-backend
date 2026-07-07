# AGENTS.md - Quy tắc riêng cho sope-backend

## 1. Vai trò trong thư mục này

Bạn đang làm việc trong phần backend của dự án SOPE.

Backend này được viết bằng **Java Spring Boot** và có cấu trúc package chính:

```text
src/main/java/com/ecommerce/ecommercebackend/
├─ config/
├─ controller/
├─ dto/
├─ entity/
├─ exception/
├─ repository/
├─ security/
├─ seeder/
├─ service/
├─ specification/
├─ util/
└─ EcommerceBackendApplication.java

src/main/resources/
```

Thư mục `sope-backend/` phụ trách:

- API cho frontend.
- API cho chatbot nếu có.
- Đăng nhập, đăng ký, xác thực, phân quyền.
- Xử lý nghiệp vụ thương mại điện tử.
- Kết nối database.
- Quản lý user, product, category, cart, order, payment nếu dự án có.
- Cung cấp dữ liệu thật cho chatbot thương mại điện tử.

Không sửa frontend hoặc chatbot từ thư mục này trừ khi yêu cầu thật sự cần và đã xác định rõ backend là nguyên nhân.

---

## 2. Quy trình bắt buộc trước khi sửa backend

Trước khi sửa code backend, phải:

1. Đọc `../CONTEXT.md` nếu tồn tại.
2. Đọc `CONTEXT.md` trong thư mục `sope-backend/` nếu tồn tại.
3. Xác định yêu cầu thuộc lớp nào:
   - API route → kiểm tra `controller/`.
   - Business logic → kiểm tra `service/`.
   - Dữ liệu request/response → kiểm tra `dto/`.
   - Bảng dữ liệu → kiểm tra `entity/`.
   - Truy vấn database → kiểm tra `repository/`.
   - Lọc/tìm kiếm động → kiểm tra `specification/`.
   - Bảo mật/xác thực → kiểm tra `security/`.
   - Cấu hình → kiểm tra `config/`.
   - Xử lý lỗi → kiểm tra `exception/`.
   - Hàm dùng chung → kiểm tra `util/`.
   - Dữ liệu khởi tạo → kiểm tra `seeder/`.
4. Kiểm tra code đã có trước khi tạo class mới.
5. Không tạo controller, service, repository, DTO, entity trùng chức năng.
6. Không tự ý đổi database schema nếu chưa được yêu cầu.
7. Không tự ý đổi response API nếu frontend/chatbot đang dùng.
8. Không tự ý bỏ security, validate hoặc exception handling để sửa lỗi nhanh.

---

## 3. Quy tắc theo từng package

### `config/`

Dùng cho cấu hình Spring Boot, CORS, OpenAPI/Swagger, bean chung, cấu hình security phụ trợ nếu có.

Khi sửa `config/`:

- Không tạo nhiều bean trùng chức năng.
- Không hard-code secret key, URL, password.
- Ưu tiên dùng `application.properties`, `application.yml` hoặc biến môi trường.
- Khi sửa CORS phải kiểm tra frontend domain, method, header, credentials.

### `controller/`

Dùng cho REST API endpoint.

Khi sửa controller:

- Controller chỉ nên nhận request, validate cơ bản, gọi service và trả response.
- Không nhồi business logic dài vào controller.
- Không truy cập repository trực tiếp trong controller nếu dự án đã dùng service layer.
- Không tạo endpoint trùng.
- Dùng đúng HTTP method:
  - `GET` để lấy dữ liệu.
  - `POST` để tạo mới hoặc xử lý hành động.
  - `PUT/PATCH` để cập nhật.
  - `DELETE` để xóa.
- Khi thêm endpoint mới, cần kiểm tra security và quyền truy cập.

### `dto/`

Dùng cho request/response object.

Khi sửa DTO:

- Không trả trực tiếp entity nếu dự án đang dùng DTO.
- Không đưa field nhạy cảm vào response DTO, ví dụ password, token secret.
- Không đổi tên field DTO nếu frontend/chatbot đang dùng mà chưa kiểm tra.
- Nếu thêm field response, phải kiểm tra nơi map dữ liệu.
- Nếu thêm request DTO, nên validate dữ liệu đầu vào nếu dự án có dùng validation.

### `entity/`

Dùng cho JPA entity ánh xạ database.

Khi sửa entity:

- Không tự ý đổi tên bảng, tên cột, quan hệ, kiểu dữ liệu nếu chưa được yêu cầu.
- Không tự ý xóa field.
- Không thay đổi quan hệ `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne` nếu chưa kiểm tra ảnh hưởng.
- Cẩn thận lỗi vòng lặp JSON khi entity quan hệ hai chiều.
- Không để password hoặc dữ liệu nhạy cảm bị serialize ra API.
- Nếu dùng Lombok, tránh `@Data` tùy tiện trên entity có quan hệ hai chiều vì dễ gây lỗi `toString`, `equals`, `hashCode`.

### `exception/`

Dùng cho xử lý lỗi tập trung.

Khi sửa exception:

- Ưu tiên dùng global exception handler nếu đã có.
- Không trả stack trace cho client.
- Message lỗi phải rõ ràng nhưng không lộ thông tin nhạy cảm.
- Không bắt lỗi chung chung rồi nuốt lỗi.
- Nếu thêm exception mới, đặt tên rõ nghĩa.

### `repository/`

Dùng cho Spring Data JPA repository.

Khi sửa repository:

- Không viết query trùng nếu method query đã có thể dùng.
- Ưu tiên method name query của Spring Data JPA nếu đơn giản.
- Với query phức tạp, dùng `@Query` rõ ràng.
- Không đưa business logic vào repository.
- Nếu cần lọc động nhiều điều kiện, ưu tiên kiểm tra `specification/`.

### `security/`

Dùng cho xác thực, phân quyền, JWT, filter, user details, password encoder, security config.

Khi sửa security:

- Không hard-code JWT secret, password, API key.
- Không log token, password, secret.
- Không bỏ kiểm tra quyền để sửa lỗi nhanh.
- Không mở public toàn bộ API nếu không được yêu cầu.
- Endpoint đăng nhập/đăng ký có thể public, endpoint quản trị phải kiểm tra role.
- Nếu sửa JWT/filter/security config, phải kiểm tra đăng nhập, refresh token nếu có, và API cần auth.
- Password phải được hash, không lưu plain text.
- Không đưa `client secret` OAuth vào frontend.

### `seeder/`

Dùng cho dữ liệu khởi tạo.

Khi sửa seeder:

- Không tạo dữ liệu trùng lặp mỗi lần chạy app.
- Kiểm tra dữ liệu đã tồn tại trước khi insert.
- Không seed dữ liệu thật hoặc nhạy cảm.
- Không xóa dữ liệu hiện có nếu chưa được yêu cầu.
- Nếu seed admin/user mẫu, mật khẩu phải được hash.

### `service/`

Dùng cho business logic.

Khi sửa service:

- Business logic chính nên nằm trong service.
- Không lặp lại logic đã có trong service khác.
- Nếu service quá dài, chỉ tách nhỏ khi thật sự cần.
- Không truy cập trực tiếp tầng controller từ service.
- Không trả entity nếu dự án đã quy ước trả DTO.
- Khi sửa logic nghiệp vụ, phải kiểm tra controller đang gọi service đó.

### `specification/`

Dùng cho truy vấn động, lọc, tìm kiếm.

Khi sửa specification:

- Không viết nhiều query rời rạc nếu có thể dùng specification.
- Kiểm tra điều kiện null/empty để tránh lỗi khi người dùng không truyền filter.
- Không làm query quá nặng nếu không cần.
- Nếu thêm filter mới, kiểm tra frontend có truyền đúng tham số không.

### `util/`

Dùng cho hàm tiện ích dùng chung.

Khi sửa util:

- Không đưa business logic riêng của một chức năng vào util.
- Không tạo util trùng chức năng.
- Hàm util nên rõ tên, ít phụ thuộc, dễ test.
- Không đặt logic bảo mật quan trọng vào util nếu đã có package `security/`.

---

## 4. Quy tắc về API response

Khi trả response API:

- Giữ cấu trúc response nhất quán với dự án.
- Không đổi format response cũ nếu frontend/chatbot đang dùng.
- Không trả dữ liệu thừa hoặc nhạy cảm.
- Nếu endpoint phục vụ chatbot, dữ liệu nên rõ ràng, ngắn gọn, dễ xử lý.

Nếu dự án đã có class response chung, phải dùng lại class đó thay vì tạo format mới.

Ví dụ response chung nếu dự án chưa có chuẩn:

```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": {}
}
```

---

## 5. Quy tắc về validate dữ liệu

Khi nhận dữ liệu từ client:

- Không tin dữ liệu từ frontend/chatbot gửi lên.
- Validate request DTO nếu có thể.
- Kiểm tra null, empty, format email, số điện thoại, giá, số lượng.
- Không để lỗi validate trở thành lỗi 500 nếu có thể trả 400.
- Message lỗi nên dễ hiểu.

Nếu dùng Bean Validation:

- Ưu tiên annotation như `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`.
- Kiểm tra controller có dùng `@Valid` chưa.

---

## 6. Quy tắc về database và entity relationship

Khi sửa phần database:

- Kiểm tra entity trước.
- Kiểm tra repository trước.
- Không tự ý đổi schema.
- Không tự ý xóa dữ liệu.
- Không tự ý reset database.
- Không tự ý đổi quan hệ giữa entity.
- Nếu cần thay đổi schema, phải báo rõ ảnh hưởng đến dữ liệu cũ, frontend và chatbot.

Không tự chạy lệnh nguy hiểm như:

- Drop database.
- Reset database.
- Xóa toàn bộ bảng.
- Xóa migration cũ nếu có.
- Seed ghi đè dữ liệu thật.

---

## 7. Quy tắc về bảo mật

Không được:

- Lưu mật khẩu dạng plain text.
- Hard-code secret key.
- Log password, token, secret.
- Trả token hoặc thông tin nhạy cảm trong lỗi.
- Mở public endpoint quản trị.
- Bỏ phân quyền để sửa lỗi nhanh.
- Đưa thông tin nhạy cảm vào DTO response.

Khi sửa auth/security, cần kiểm tra:

- Đăng ký.
- Đăng nhập.
- Token/JWT nếu có.
- Endpoint cần đăng nhập.
- Endpoint cần role admin/user.
- CORS nếu frontend không gọi được API.

---

## 8. Quy tắc khi backend phục vụ frontend

Khi sửa API cho frontend:

- Kiểm tra frontend đang gọi endpoint nào.
- Kiểm tra method, URL, request body, query params.
- Kiểm tra response frontend đang đọc field nào.
- Không đổi tên field response nếu frontend đang dùng.
- Nếu bắt buộc đổi response, phải ghi rõ frontend cần sửa file nào.

---

## 9. Quy tắc khi backend phục vụ chatbot

Khi sửa API cho chatbot:

- Kiểm tra chatbot đang gọi endpoint nào.
- Chỉ trả dữ liệu cần thiết cho chatbot.
- Không trả thông tin nhạy cảm của người dùng.
- Nếu chatbot hỏi sản phẩm, API nên trả dữ liệu thật: tên, mô tả, giá, danh mục, tồn kho nếu có.
- Không để chatbot phải xử lý response quá phức tạp nếu không cần.

---

## 10. Quy tắc kiểm tra backend

Sau khi sửa backend, nếu phù hợp, hãy chạy hoặc đề xuất chạy:

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

Nếu dự án dùng Maven cài sẵn trên máy:

```bash
mvn spring-boot:run
mvn test
mvn clean package
```

Nếu dự án dùng Gradle, dùng:

```bash
./gradlew bootRun
./gradlew test
./gradlew build
```

Khi báo lại, phải nêu rõ:

- Endpoint cần test.
- Method.
- URL.
- Headers nếu có.
- Body mẫu nếu cần.
- Kết quả mong đợi.
- Có ảnh hưởng frontend/chatbot không.

---

## 11. Những việc không được tự ý làm

Không tự ý:

- Đổi package name `com.ecommerce.ecommercebackend`.
- Đổi tên class main `EcommerceBackendApplication`.
- Đổi toàn bộ kiến trúc Spring Boot.
- Xóa package đang có.
- Tạo package mới nếu package hiện tại đã đủ.
- Đổi database schema.
- Xóa dữ liệu.
- Bỏ security.
- Bỏ validate.
- Hard-code secret key.
- Đổi toàn bộ response API.
- Tạo endpoint trùng.
- Sửa nhiều module không liên quan.
- Cài dependency mới nếu chưa kiểm tra dependency hiện có.

---

## 12. Cập nhật CONTEXT.md

Sau khi sửa backend, phải cập nhật:

1. `sope-backend/CONTEXT.md`
2. `../CONTEXT.md` nếu thay đổi quan trọng ảnh hưởng toàn dự án.

Nội dung cập nhật gồm:

- Yêu cầu backend vừa xử lý.
- Package liên quan: controller, service, repository, entity, dto, security...
- File đã sửa.
- Endpoint liên quan.
- Database/entity liên quan nếu có.
- Có ảnh hưởng frontend/chatbot không.
- Cách kiểm tra API.
- Lỗi cần tránh lặp lại nếu có.
- Việc cần làm tiếp theo.

Không chép code dài vào `CONTEXT.md`.
Chỉ ghi tóm tắt ngắn gọn, đủ để lần sau Codex hiểu và làm tiếp.

---

## 13. Cách trả lời người dùng sau khi hoàn thành

Sau khi làm xong, trả lời bằng tiếng Việt theo mẫu:

### Đã hoàn thành

- Đã làm:
- File đã sửa:
- Endpoint liên quan:
- Cách kiểm tra:
- Đã cập nhật `CONTEXT.md`:

### Lưu ý

- Nêu ngắn gọn lỗi hoặc rủi ro nếu có.
- Nêu việc nên làm tiếp theo nếu cần.

Không trả lời quá dài nếu nhiệm vụ nhỏ.
