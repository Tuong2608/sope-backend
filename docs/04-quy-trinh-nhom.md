# 🤝 Quy Trình Làm Việc Nhóm

## Nguyên tắc chung

- **Luôn pull trước khi làm việc** để tránh conflict.
- **Không làm việc trực tiếp trên nhánh `main`** — tạo nhánh riêng cho từng tính năng.
- **Commit thường xuyên** với message rõ ràng, có nghĩa.
- **Không commit file cấu hình cá nhân** (database password, v.v.).

---

## 🌿 Quy trình làm việc (Git Flow)

### Bước 1 — Cập nhật code mới nhất từ `main`

```bash
git checkout main
git pull origin main
```

---

### Bước 2 — Tạo nhánh riêng để làm tính năng

Đặt tên nhánh theo format: `<loai>/<ten-tinh-nang>`

```bash
# Tính năng mới
git checkout -b feature/ten-tinh-nang

# Sửa lỗi
git checkout -b fix/ten-loi

# Ví dụ thực tế
git checkout -b feature/them-gio-hang
git checkout -b fix/loi-dang-nhap
```

---

### Bước 3 — Code và commit

```bash
# Kiểm tra file đã thay đổi
git status

# Thêm file vào staging
git add .                         # tất cả file
git add src/main/java/...         # chỉ file cụ thể

# Commit với message rõ ràng
git commit -m "feat: thêm chức năng giỏ hàng"
```

---

### Bước 4 — Push nhánh lên GitHub

```bash
git push origin feature/ten-tinh-nang
```

---

### Bước 5 — Tạo Pull Request (PR)

1. Vào **https://github.com/Tuong2608/sope-backend**
2. GitHub sẽ hiện nút **"Compare & pull request"** — click vào
3. Điền mô tả thay đổi
4. Chọn reviewer (nếu có)
5. Click **"Create pull request"**

---

### Bước 6 — Merge vào `main`

Sau khi được review và approve → **Merge** PR vào `main`.

---

## 📝 Quy tắc đặt tên Commit

Dùng **Conventional Commits** để dễ đọc lịch sử:

| Loại | Khi nào dùng | Ví dụ |
|------|-------------|-------|
| `feat:` | Thêm tính năng mới | `feat: them api gio hang` |
| `fix:` | Sửa bug | `fix: loi khong tra ve token` |
| `docs:` | Cập nhật tài liệu | `docs: them huong dan cai dat` |
| `refactor:` | Tái cấu trúc code | `refactor: tach service gio hang` |
| `style:` | Format code (không đổi logic) | `style: chinh sua indent` |
| `test:` | Thêm/sửa test | `test: them unit test auth` |
| `chore:` | Cập nhật cấu hình, build | `chore: cap nhat pom.xml` |

---

## ⚡ Lệnh Git thường dùng

```bash
# Xem trạng thái các file
git status

# Xem lịch sử commit (gọn)
git log --oneline -10

# Xem các nhánh hiện có
git branch -a

# Chuyển nhánh
git checkout ten-nhanh

# Xoá nhánh (sau khi merge xong)
git branch -d feature/ten-tinh-nang           # local
git push origin --delete feature/ten-tinh-nang # remote

# Kéo code mới từ remote về nhánh hiện tại
git pull origin main

# Huỷ thay đổi chưa commit (cẩn thận!)
git checkout -- .

# Stash (lưu tạm thay đổi chưa commit)
git stash
git stash pop    # lấy lại
```

---

## ⚠️ File KHÔNG được commit lên GitHub

Đã được thêm vào `.gitignore`, nhưng cần chú ý:

- `application.properties` nếu có chứa password thật
- Thư mục `target/` (build output)
- Thư mục `.idea/` (cấu hình IntelliJ)
- File `.env` nếu có

---

## 🆘 Xử lý conflict

Khi bị conflict sau khi pull:

```bash
# 1. Xem file bị conflict
git status

# 2. Mở file conflict, tìm và sửa phần được đánh dấu:
# <<<<<<< HEAD
# code của bạn
# =======
# code từ remote
# >>>>>>> origin/main

# 3. Sau khi sửa xong, add và commit
git add .
git commit -m "fix: resolve merge conflict"
```

> 💡 **Tip**: Dùng **VS Code** để resolve conflict dễ hơn — nó có giao diện trực quan để chọn "Accept Current Change" hoặc "Accept Incoming Change".
