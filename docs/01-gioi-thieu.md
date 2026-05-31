# 📖 Giới Thiệu Dự Án

## SOPE Backend

Backend RESTful API cho dự án SOPE, xây dựng bằng **Spring Boot 4**, **MySQL** và xác thực bằng **JWT Token**.

---

## 🧰 Công nghệ sử dụng

| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|----------|
| Java | 17 | Ngôn ngữ lập trình chính |
| Spring Boot | 4.0.6 | Framework backend |
| Spring Security | 6.x | Xác thực & phân quyền |
| Spring Data JPA | 3.x | Tương tác database |
| MySQL | 8.0+ | Cơ sở dữ liệu |
| JJWT | 0.11.5 | Tạo và xác thực JWT Token |
| Lombok | Latest | Giảm boilerplate code |
| Maven | 3.8+ | Quản lý dependencies |

---

## 📁 Cấu trúc thư mục

```
sope-backend/
├── docs/                            # 📂 Tài liệu dự án (bạn đang đọc)
│   ├── 01-gioi-thieu.md
│   ├── 02-cai-dat-va-chay.md
│   ├── 03-api-reference.md
│   └── 04-quy-trinh-nhom.md
├── src/
│   └── main/
│       ├── java/com/ecommerce/ecommercebackend/
│       │   ├── config/              # Cấu hình Spring Security
│       │   ├── controller/          # REST Controllers (API endpoints)
│       │   ├── dto/                 # Request & Response objects
│       │   │   ├── request/         # Dữ liệu đầu vào từ client
│       │   │   └── response/        # Dữ liệu trả về cho client
│       │   ├── entity/              # JPA Entities (mapping bảng DB)
│       │   ├── exception/           # Xử lý lỗi toàn cục
│       │   ├── repository/          # Spring Data JPA Repositories
│       │   ├── security/            # JWT Filter, UserDetailsService
│       │   ├── service/             # Business Logic
│       │   ├── specification/       # JPA Specifications (dynamic query)
│       │   └── util/                # Các tiện ích dùng chung
│       └── resources/
│           └── application.properties  # File cấu hình ứng dụng
├── pom.xml                          # Maven dependencies
└── README.md                        # Hướng dẫn nhanh
```

---

## 📚 Tài liệu khác

| File | Nội dung |
|------|----------|
| [02-cai-dat-va-chay.md](./02-cai-dat-va-chay.md) | Hướng dẫn cài đặt môi trường và chạy project |
| [03-api-reference.md](./03-api-reference.md) | Danh sách đầy đủ các API endpoint |
| [04-quy-trinh-nhom.md](./04-quy-trinh-nhom.md) | Quy trình làm việc nhóm với Git |
