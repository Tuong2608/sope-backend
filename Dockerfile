# GIAI ĐOẠN 1: Build mã nguồn bằng JDK 17 và Maven Wrapper
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Sao chép các file cấu hình Maven Wrapper trước để tận dụng Docker Cache cho dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Xử lý ký tự xuống dòng (CRLF -> LF) của file mvnw (Cực kỳ quan trọng khi build trên Windows)
# Nếu không xử lý, Linux trong Docker sẽ bị lỗi không chạy được file mvnw
RUN tr -d '\r' < mvnw > mvnw.tmp && mv mvnw.tmp mvnw && chmod +x mvnw

# Tải trước các dependencies Maven về máy ảo Docker (Go offline) để tăng tốc độ build các lần sau
RUN ./mvnw dependency:go-offline -B

# Sao chép toàn bộ mã nguồn vào trong container
COPY src ./src

# Tiến hành đóng gói code thành file .jar (Bỏ qua chạy Unit Test để build nhanh hơn)
RUN ./mvnw clean package -DskipTests

# GIAI ĐOẠN 2: Chạy ứng dụng bằng JRE 17 tối giản (Nhẹ và Bảo mật)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Sao chép file .jar đã đóng gói từ Giai đoạn 1 sang Giai đoạn 2
COPY --from=build /app/target/*.jar app.jar

# Khai báo cổng ứng dụng Spring Boot chạy (mặc định là 8080)
EXPOSE 8080

# Lệnh khởi động Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
