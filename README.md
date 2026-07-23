# 🛍️ SOPE Backend

Backend RESTful API cho dự án SOPE, xây dựng bằng **Spring Boot 4**, **MySQL** và **JWT Authentication**.

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

```bash
# 1. Clone
git clone git@github.com:Tuong2608/sope-backend.git
cd sope-backend

# 2. Cấu hình database trong:
# src/main/resources/application.properties

# 3. Chạy
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run           # Linux / macOS
```

🟢 Server chạy tại: `http://localhost:8080`

> Xem chi tiết tại [docs/02-cai-dat-va-chay.md](./docs/02-cai-dat-va-chay.md)

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
