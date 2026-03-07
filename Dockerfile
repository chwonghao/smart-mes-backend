# Bước 1: Sử dụng hình ảnh JDK 17 làm nền tảng
FROM eclipse-temurin:17-jdk-alpine

# Bước 2: Tạo thư mục làm việc trong container
WORKDIR /app

# Bước 3: Copy file jar đã build từ máy vào container
# Lưu ý: Tên file jar phải khớp với tên trong pom.xml (mes-backend-0.0.1-SNAPSHOT.jar)
COPY target/*.jar app.jar

# Bước 4: Khai báo cổng mà ứng dụng sẽ chạy
EXPOSE 8080

# Bước 5: Lệnh để khởi chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]