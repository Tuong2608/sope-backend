# 🚀 Cài Đặt & Chạy Backend

## 1. Yêu cầu cài đặt

Trước khi chạy project, hãy đảm bảo máy bạn đã cài đầy đủ:

| Công cụ | Phiên bản | Link tải |
|---------|-----------|----------|
| **JDK** | 17 trở lên | https://www.oracle.com/java/technologies/downloads/#java17 |
| **MySQL** | 8.0+ | https://dev.mysql.com/downloads/mysql/ |
| **Git** | Mới nhất | https://git-scm.com/downloads |
| **Maven** | 3.8+ *(tuỳ chọn)* | https://maven.apache.org/download.cgi |

> 💡 **Kiểm tra đã cài chưa:**
> ```bash
> java -version
> mysql --version
> git --version
> ```

---

## 2. Clone repository

```bash
# Dùng SSH (cần SSH key đã thêm vào GitHub)
git clone git@github.com:Tuong2608/sope-backend.git

# Hoặc dùng HTTPS (không cần SSH key)
git clone https://github.com/Tuong2608/sope-backend.git

# Di chuyển vào thư mục project
cd sope-backend
```

---

## 3. Cấu hình Database

### Bước 3.1 — Khởi động MySQL

Đảm bảo MySQL đang chạy trên máy bạn (port mặc định `3306`).

### Bước 3.2 — Chỉnh file cấu hình

Mở file: `src/main/resources/application.properties`

Tìm và sửa các dòng sau theo thông tin MySQL của bạn:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root        ← đổi thành username của bạn
spring.datasource.password=root        ← đổi thành password của bạn
```

> ⚠️ **Lưu ý quan trọng:**
> - Database `ecommerce_db` sẽ được **tự động tạo** khi khởi động lần đầu.
> - **Không commit** file `application.properties` sau khi đã điền thông tin cá nhân!

---

## 4. Chạy Backend

### ✅ Cách 1 — Maven Wrapper (khuyên dùng, không cần cài Maven)

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### ✅ Cách 2 — Maven đã cài sẵn

```bash
mvn spring-boot:run
```

### ✅ Cách 3 — Build file JAR rồi chạy

```bash
# Build (bỏ qua test)
mvn clean package -DskipTests

# Chạy file JAR
java -jar target/sope-backend-0.0.1-SNAPSHOT.jar
```

---

## 5. Xác nhận chạy thành công

Khi khởi động thành công, bạn sẽ thấy log tương tự:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

...
Started EcommerceBackendApplication in 5.123 seconds (JVM running for 5.8)
```

🟢 **Server đang chạy tại:** `http://localhost:8080`

---

## 6. Test nhanh API

Mở trình duyệt hoặc Postman, thử gọi:

```
GET http://localhost:8080/api/products
```

Nếu nhận được response JSON → backend đang hoạt động bình thường! ✅

---

## 7. Lỗi thường gặp & cách xử lý

### ❌ `Access denied for user 'root'@'localhost'`
**Nguyên nhân:** Sai username hoặc password MySQL.  
**Cách fix:** Kiểm tra lại thông tin trong `application.properties`.

---

### ❌ `Communications link failure`
**Nguyên nhân:** MySQL chưa được khởi động.  
**Cách fix:** Mở MySQL Workbench hoặc khởi động MySQL Service.

```bash
# Windows (chạy với quyền Admin)
net start MySQL80

# macOS
brew services start mysql

# Linux
sudo systemctl start mysql
```

---

### ❌ `Web server failed to start. Port 8080 was already in use`
**Nguyên nhân:** Port 8080 đang bị chiếm bởi ứng dụng khác.  
**Cách fix:** Thêm dòng sau vào `application.properties`:

```properties
server.port=8081
```

---

### ❌ `java: error: release version 17 not supported`
**Nguyên nhân:** JDK cài chưa đúng phiên bản (cần JDK 17+).  
**Cách fix:** Tải và cài JDK 17 từ https://www.oracle.com/java/technologies/downloads/#java17, sau đó kiểm tra:

```bash
java -version
# Phải thấy: openjdk version "17.x.x"
```

---

### ❌ `mvnw.cmd: Permission denied` (Linux/macOS)
**Cách fix:**
```bash
chmod +x mvnw
./mvnw spring-boot:run
```
