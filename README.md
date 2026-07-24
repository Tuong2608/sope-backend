# 🛍️ SOPE Backend

Backend RESTful API cho dự án SOPE, xây dựng bằng **Spring Boot 4**, **MySQL** và **JWT Authentication**.

## Mô tả dự án

SOPE Backend là trung tâm nghiệp vụ và bảo mật của toàn hệ thống:

- Cung cấp REST API cho Next.js frontend.
- Quản lý người dùng, JWT/cookie, role user/admin và Google Login.
- Quản lý catalog, tìm kiếm/lọc sản phẩm, tồn kho và biến thể.
- Xử lý giỏ hàng, coupon, giao hàng, đơn hàng và trạng thái đơn.
- Tích hợp thanh toán COD, VNPAY và MoMo.
- Phát thông báo đơn hàng realtime qua WebSocket/STOMP.
- Proxy chat từ frontend sang FastAPI; dữ liệu đơn cá nhân vẫn được xử lý ở
  Spring Boot và không gửi sang LLM.
- Gọi recommendation như tính năng phụ; timeout sẽ fallback danh sách rỗng.
- Cấp endpoint catalog projection nhẹ, bảo vệ bằng service key, cho chatbot.

Kiến trúc triển khai:

```text
Browser
  → Next.js Frontend
  → Spring Boot Backend
      ├── MySQL
      ├── FastAPI Chatbot → Gemini
      └── VNPAY/MoMo/SMTP
```

Các service production:

| Thành phần | Link deploy |
|---|---|
| Website SOPE | [https://sope-frontend-self.vercel.app/](https://sope-frontend-self.vercel.app/) |
| Spring Boot API | [https://sope-backend-wezh.onrender.com/](https://sope-backend-wezh.onrender.com/) |
| FastAPI Chatbot | [https://chatbot-tmdt.onrender.com/](https://chatbot-tmdt.onrender.com/) |

## Tài khoản admin mặc định

| Thông tin | Giá trị |
|---|---|
| Tên đăng nhập | `admin` |
| Mật khẩu | `admin123` |

Backend đọc thông tin khởi tạo admin từ `APP_ADMIN_USERNAME` và
`APP_ADMIN_PASSWORD`. Giá trị trên chỉ dành cho local/demo; khi deploy
production phải thay `APP_ADMIN_PASSWORD` bằng mật khẩu mạnh và không commit
credential production vào Git.

## Cấu trúc dự án

```text
sope-backend/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/ecommercebackend/
│   │   │   ├── config/          # Bean, CORS, WebSocket, HTTP client
│   │   │   ├── controller/      # REST API và controller admin
│   │   │   ├── dto/             # Request/response DTO
│   │   │   ├── entity/          # JPA entity và enum nghiệp vụ
│   │   │   ├── exception/       # Custom/global exception handling
│   │   │   ├── repository/      # Spring Data JPA query
│   │   │   ├── security/        # JWT, filter và authentication
│   │   │   ├── seeder/          # Dữ liệu khởi tạo idempotent
│   │   │   ├── service/         # Business logic
│   │   │   ├── specification/   # Product filter/search động
│   │   │   ├── util/            # Helper dùng chung
│   │   │   └── EcommerceBackendApplication.java
│   │   └── resources/
│   │       ├── db/              # Flyway migration
│   │       ├── application.properties
│   │       └── data.json        # Catalog seed
│   └── test/                    # Unit và integration test
├── docs/                        # Tài liệu cài đặt/API/workflow
├── .env.example                 # Mẫu environment
├── Dockerfile                   # Java 17 multi-stage image
├── pom.xml                      # Maven dependency/build config
├── mvnw / mvnw.cmd              # Maven Wrapper
└── README.md
```

Vai trò từng package:

| Package | Chức năng |
|---|---|
| `config` | CORS, WebSocket, RestTemplate, bean và cấu hình framework |
| `controller` | Nhận HTTP request, validate và gọi service |
| `dto` | Contract request/response, không trả entity trực tiếp |
| `entity` | Mô hình dữ liệu MySQL qua JPA/Hibernate |
| `exception` | Chuyển exception thành JSON response có kiểm soát |
| `repository` | Truy vấn database và projection |
| `security` | JWT, cookie, filter, role và rate limiting |
| `seeder` | Seed admin, catalog, lịch nghỉ và vận chuyển |
| `service` | Nghiệp vụ auth, product, cart, order, payment, chat |
| `specification` | Ghép điều kiện tìm kiếm/lọc product |
| `util` | Hàm format/parse/ảnh dùng chung |

---

## 📚 Tài liệu

| # | File | Nội dung |
|---|------|----------|
| 1 | [Giới thiệu dự án](./docs/01-gioi-thieu.md) | Công nghệ sử dụng, cấu trúc thư mục |
| 2 | [Cài đặt & Chạy backend](./docs/02-cai-dat-va-chay.md) | Hướng dẫn cài đặt môi trường, cấu hình, chạy project và xử lý lỗi |
| 3 | [API Reference](./docs/03-api-reference.md) | Danh sách đầy đủ API endpoint, request/response mẫu |
| 4 | [Quy trình làm việc nhóm](./docs/04-quy-trinh-nhom.md) | Git flow, quy tắc commit, xử lý conflict |

---

## ⚡ Chạy nhanh

Yêu cầu:

- Java 17.
- MySQL 8.
- Chatbot tại `http://localhost:8000` nếu cần dùng chat/recommendation.

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cp .env.example .env
./mvnw spring-boot:run
```

Khi chạy Maven trực tiếp, cấu hình secret theo một trong hai cách:

1. Đặt environment variables trong IDE/terminal.
2. Tạo file `application-secrets.properties` tại root `sope-backend/`.

`application.properties` đã import file local này bằng:

```properties
spring.config.import=optional:file:./application-secrets.properties
```

Ví dụ nội dung local:

```properties
spring.datasource.username=root
spring.datasource.password=<your-mysql-password>
app.jwt.secret=<base64-secret-at-least-32-bytes>
app.admin.password=<your-local-admin-password>
app.chatbot.secret=<same-value-as-chatbot-SOPE_SERVICE_KEY>
```

`application-secrets.properties` đã được Git ignore. Không commit file này,
không copy secret thật vào `application.properties`. File `.env` chỉ được Docker
Compose/`docker run --env-file` đọc, Spring Boot không tự đọc `.env` khi chạy
Maven trực tiếp.

Server mặc định:

```text
http://localhost:8080
```

Health:

```powershell
Invoke-RestMethod "http://localhost:8080/api/health"
```

Test và build:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

Xem hướng dẫn chi tiết tại
[docs/02-cai-dat-va-chay.md](./docs/02-cai-dat-va-chay.md).

## Deploy

File `Dockerfile` tạo image Java 17 theo multi-stage build, chạy bằng user
không có quyền root và kiểm tra health tại `/api/health`.

Để deploy đủ MySQL + Backend + Chatbot + Frontend, dùng
`../docker-compose.yml`, `../.env.example` và hướng dẫn
`../DEPLOYMENT.md` từ root workspace. Không copy
`application-secrets.properties` vào image; production phải truyền secret bằng
environment/secret manager.

Deploy backend riêng:

```bash
docker build -t sope-backend .
docker run --rm -p 8080:8080 --env-file .env sope-backend
```

Các biến bắt buộc tối thiểu gồm database, `APP_JWT_SECRET`,
`APP_ADMIN_PASSWORD`, `APP_FRONTEND_ORIGINS`, `CHATBOT_URL` và
`CHATBOT_SECRET`.

Production:

- Backend: [https://sope-backend-wezh.onrender.com/](https://sope-backend-wezh.onrender.com/)
- Website: [https://sope-frontend-self.vercel.app/](https://sope-frontend-self.vercel.app/)
