# 💳 UML Sequence Diagram — Luồng Thanh Toán Online

Tài liệu này mô tả luồng xử lý thanh toán qua **VNPAY** và **MoMo Sandbox** theo chuẩn UML Sequence Diagram.

---

## 1. Luồng Thanh Toán VNPAY

```mermaid
sequenceDiagram
    actor User as 👤 Người dùng
    participant FE as 🖥️ Frontend (Next.js)
    participant BE as ⚙️ Backend (Spring Boot)
    participant DB as 🗄️ Database (MySQL)
    participant VN as 🏦 VNPAY Sandbox

    User->>FE: Nhấn "Thanh toán" (chọn VNPAY)
    FE->>BE: POST /api/payment/create\n{ orderId, amount, provider: "VNPAY" }
    BE->>BE: Tạo chữ ký HMAC-SHA512\nvới secret key VNPAY
    BE->>DB: Lưu Payment (status: PENDING)
    DB-->>BE: Payment đã lưu (id)
    BE-->>FE: { paymentUrl, paymentId, status: "PENDING" }
    FE->>User: Chuyển hướng tới VNPAY
    User->>VN: Điền thông tin thẻ & xác nhận
    VN->>VN: Xử lý giao dịch

    alt Thanh toán thành công
        VN->>BE: POST /api/payment/vnpay/ipn\n(IPN - server to server)\n{ vnp_ResponseCode: "00", chữ ký }
        BE->>BE: Xác thực chữ ký HMAC-SHA512
        BE->>DB: Cập nhật Payment → status: SUCCESS
        DB-->>BE: OK
        BE-->>VN: { RspCode: "00", Message: "Confirm Success" }
        VN->>FE: Redirect GET /api/payment/vnpay/callback\n?vnp_ResponseCode=00&...
        FE-->>User: ✅ Hiển thị trang thanh toán thành công
    else Thanh toán thất bại / huỷ
        VN->>BE: POST /api/payment/vnpay/ipn\n{ vnp_ResponseCode: "24" (huỷ) }
        BE->>BE: Xác thực chữ ký
        BE->>DB: Cập nhật Payment → status: FAILED
        DB-->>BE: OK
        BE-->>VN: { RspCode: "00", Message: "Confirm Success" }
        VN->>FE: Redirect GET /api/payment/vnpay/callback\n?vnp_ResponseCode=24
        FE-->>User: ❌ Hiển thị trang thanh toán thất bại
    end
```

### Giải thích các bước VNPAY

| Bước | Diễn giải |
|------|-----------|
| **1. Tạo link** | Backend tạo URL thanh toán với tham số được ký bằng HMAC-SHA512, lưu Payment vào DB với trạng thái `PENDING` |
| **2. Redirect** | Frontend chuyển hướng người dùng sang trang thanh toán VNPAY Sandbox |
| **3. IPN (server-to-server)** | VNPAY gọi thẳng vào backend để xác nhận kết quả — **đây là kênh đáng tin cậy nhất** |
| **4. Verify chữ ký** | Backend xác thực chữ ký để đảm bảo dữ liệu không bị giả mạo |
| **5. Cập nhật DB** | Backend cập nhật trạng thái Payment trong database |
| **6. Callback** | VNPAY redirect trình duyệt về frontend để hiển thị kết quả cho người dùng |

> ⚠️ **Quan trọng:** Chỉ cập nhật trạng thái đơn hàng sau khi xác thực IPN thành công, **không** dựa vào callback URL (vì người dùng có thể tắt trình duyệt trước khi callback).

---

## 2. Luồng Thanh Toán MoMo

```mermaid
sequenceDiagram
    actor User as 👤 Người dùng
    participant FE as 🖥️ Frontend (Next.js)
    participant BE as ⚙️ Backend (Spring Boot)
    participant DB as 🗄️ Database (MySQL)
    participant MM as 📱 MoMo Sandbox

    User->>FE: Nhấn "Thanh toán" (chọn MoMo)
    FE->>BE: POST /api/payment/create\n{ orderId, amount, provider: "MOMO" }
    BE->>BE: Tạo chữ ký HMAC-SHA256\nvới accessKey + secretKey MoMo
    BE->>MM: POST https://test-payment.momo.vn/v2/gateway/api/create\n{ partnerCode, requestId, amount, signature, ... }
    MM->>MM: Xác thực yêu cầu
    MM-->>BE: { resultCode: 0, payUrl: "https://..." }
    BE->>DB: Lưu Payment (status: PENDING)
    DB-->>BE: Payment đã lưu (id)
    BE-->>FE: { paymentUrl, paymentId, status: "PENDING" }
    FE->>User: Chuyển hướng tới MoMo
    User->>MM: Quét QR / xác nhận trên app MoMo
    MM->>MM: Xử lý giao dịch

    alt Thanh toán thành công
        MM->>BE: POST /api/payment/momo/ipn\n(IPN - server to server)\n{ resultCode: 0, chữ ký }
        BE->>BE: Xác thực chữ ký HMAC-SHA256
        BE->>DB: Cập nhật Payment → status: SUCCESS
        DB-->>BE: OK
        BE-->>MM: HTTP 204 No Content
        MM->>FE: Redirect GET /api/payment/momo/callback\n?resultCode=0&...
        FE-->>User: ✅ Hiển thị trang thanh toán thành công
    else Thanh toán thất bại / huỷ
        MM->>BE: POST /api/payment/momo/ipn\n{ resultCode: 1006 (người dùng huỷ) }
        BE->>BE: Xác thực chữ ký
        BE->>DB: Cập nhật Payment → status: FAILED
        DB-->>BE: OK
        BE-->>MM: HTTP 204 No Content
        MM->>FE: Redirect GET /api/payment/momo/callback\n?resultCode=1006
        FE-->>User: ❌ Hiển thị trang thanh toán thất bại
    end
```

### Giải thích các bước MoMo

| Bước | Diễn giải |
|------|-----------|
| **1. Tạo yêu cầu** | Backend gọi API MoMo để tạo giao dịch, nhận về `payUrl` |
| **2. Ký HMAC-SHA256** | Tất cả tham số được ký bằng `secretKey` trước khi gửi cho MoMo |
| **3. Lưu PENDING** | Backend lưu Payment vào DB với trạng thái `PENDING` |
| **4. Redirect** | Frontend chuyển hướng người dùng sang trang thanh toán MoMo |
| **5. IPN (notify_url)** | MoMo gọi `notify_url` để thông báo kết quả — kênh đáng tin cậy |
| **6. Verify + Update** | Backend xác thực chữ ký và cập nhật trạng thái trong DB |
| **7. Callback** | MoMo redirect trình duyệt về `return_url` trên frontend |

### MoMo Result Codes thường gặp

| Code | Ý nghĩa |
|------|---------|
| `0` | Thành công |
| `1006` | Người dùng huỷ giao dịch |
| `1005` | Hết hạn giao dịch |
| `11` | Truy cập bị từ chối (sai chữ ký) |
| `99` | Lỗi không xác định |

---

## 3. So sánh VNPAY vs MoMo

| Tiêu chí | VNPAY | MoMo |
|----------|-------|------|
| **Phương thức ký** | HMAC-SHA512 | HMAC-SHA256 |
| **Tạo link** | Backend tự build URL với query params | Backend gọi API MoMo → nhận `payUrl` |
| **IPN endpoint** | `/api/payment/vnpay/ipn` (GET hoặc POST) | `/api/payment/momo/ipn` (POST) |
| **Success code** | `vnp_ResponseCode = "00"` | `resultCode = 0` |
| **Phương thức thanh toán** | ATM, thẻ quốc tế, QR | Ví MoMo, QR |
| **Sandbox** | Có (thẻ test cố định) | Có (tài khoản test) |

---

## 4. Bảng thanh toán trong DB (`payments`)

```
payments
├── id              BIGINT PK AUTO_INCREMENT
├── order_id        VARCHAR(100)     -- mã đơn hàng
├── amount          BIGINT           -- số tiền VND
├── provider        ENUM('VNPAY','MOMO')
├── status          ENUM('PENDING','SUCCESS','FAILED','REFUNDED')
├── transaction_id  VARCHAR(100)     -- mã GD từ cổng thanh toán
├── payment_url     TEXT             -- URL thanh toán
├── created_at      DATETIME
└── updated_at      DATETIME
```
