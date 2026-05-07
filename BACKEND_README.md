# 🏭 SmartMES - Hệ Thống Điều Hành Quản Lý Sản Xuất (Backend)

Chào mừng đến với kho lưu trữ mã nguồn Backend của **SmartMES**. Đây là hệ thống lõi cung cấp các dịch vụ quản lý sản xuất, kiểm soát kho, và giám sát thiết bị theo thời gian thực dành cho nhà máy thông minh.

## 🛠 Công Nghệ Sử Dụng (Tech Stack)

- **Ngôn ngữ:** Java 17
- **Framework:** Spring Boot 3.4.x
- **Cơ sở dữ liệu:** PostgreSQL
- **ORM:** Hibernate / Spring Data JPA
- **Xác thực:** Spring Security & JWT (JSON Web Tokens) cấu hình qua `HttpOnly Cookie`
- **Real-time:** WebSockets (STOMP Protocol)

---

## 🏗 Kiến Trúc Module

Hệ thống được thiết kế theo kiến trúc Module hóa (Modular Architecture), chia thành các miền nghiệp vụ (Domain) rõ ràng:

### 1. `auth` - Module Xác Thực & Phân Quyền
- Quản lý đăng nhập/đăng xuất và phân quyền người dùng.
- **Bảo mật cao:** Sử dụng cơ chế lưu trữ JWT Token trong `HttpOnly Cookie` với các cờ `Secure`, `SameSite=Lax`. Token không bao giờ bị lộ ra bộ nhớ trình duyệt, ngăn chặn hoàn toàn tấn công XSS.
- Tự động xử lý và làm sạch session khi token hết hạn (`ExpiredJwtException`), trả về mã `401 Unauthorized`.

### 2. `masterdata` - Module Dữ Liệu Nền Tảng
Quản lý danh mục cốt lõi làm nền tảng cho việc lập kế hoạch sản xuất:
- **ItemMaster (Sản phẩm/Vật tư):** Quản lý định danh, đơn vị tính. Hỗ trợ trường dữ liệu động qua cột `JSONB` của PostgreSQL (`custom_attributes`), cho phép lưu các thuộc tính tuỳ biến không giới hạn.
- **WorkCenter (Máy móc/Trạm sản xuất):** Quản lý thiết bị, công suất (hourly capacity), và trạng thái hoạt động hiện tại (RUNNING, DOWN, IDLE...).
- **BOM (Định mức vật tư):** Quản lý công thức/cấu trúc cấu thành nên sản phẩm.
- **Routing (Quy trình sản xuất):** Định nghĩa trình tự các bước gia công trên từng máy.
- **Worker:** Thông tin công nhân, ca kíp.

### 3. `production` - Module Quản Lý Sản Xuất (Lõi)
- **WorkOrder (Lệnh sản xuất):** Quản lý thông tin tổng quan của lệnh (mục tiêu, thời gian).
- **ProductionSchedule (Lịch sản xuất chi tiết):** (Cấu trúc 1-N). Một Lệnh sản xuất có thể được phân bổ để chạy qua nhiều máy móc (Work Centers) theo thứ tự công đoạn.
- **ProductionLog & QualityCheck:** Ghi nhận sản lượng đạt (Pass), sản lượng hỏng (NG), lý do lỗi và người thao tác tại từng máy.
- **Tính năng Idempotency (Chống trùng lặp):** API báo cáo sản lượng có tích hợp mã `requestId` (UUID) sinh ra từ phía Client. Khi Client gặp lỗi mạng và thử gửi lại cùng một báo cáo, Backend sẽ phát hiện UUID đã tồn tại và bỏ qua, tránh tình trạng cộng dồn khống sản lượng.

### 4. `inventory` - Module Quản Lý Kho
- Giao tiếp chặt chẽ với module Production.
- Mỗi khi có báo cáo sản lượng hoàn thành:
  - Tự động **cộng thêm** tồn kho cho Thành phẩm (Finished Goods).
  - Tự động **trừ lùi** (Backflush) tồn kho Nguyên vật liệu (Raw Materials) theo định mức BOM và tỷ lệ hao hụt (Scrap Factor).

### 5. `realtime` - Module Giám Sát Thời Gian Thực
- Tích hợp WebSocket (`/ws-mes`).
- Broadcast các sự kiện trực tiếp tới Dashboard của ban quản đốc:
  - `NEW_ORDER`: Khi có lệnh mới tạo.
  - `PROGRESS_UPDATED`: Khi sản lượng vừa được công nhân báo cáo qua mã QR.
  - `INVENTORY_UPDATED`: Khi tồn kho thay đổi.
  - `MACHINE_STATUS_CHANGED`: Cập nhật trạng thái máy móc.
- Tích hợp hệ thống Cảnh báo (`AlertService`) qua topic `/topic/alerts` khi tỷ lệ lỗi vượt ngưỡng hoặc máy bị báo hỏng.

---

## 💡 Các Tính Năng Kỹ Thuật Nổi Bật

### Kiến trúc Multi-Tenant (Đa Người Thuê)
Tất cả các truy vấn dữ liệu, lưu trữ, và nghiệp vụ đều được gắn kèm và kiểm tra nghiêm ngặt bằng biến `tenantId`. Đảm bảo dữ liệu của khách hàng/chi nhánh nào chỉ có thể được truy xuất bởi tài khoản thuộc chi nhánh đó, tránh rò rỉ dữ liệu chéo (`Access denied`).

### Audit Logging (Truy Vết Hành Động)
Sử dụng Custom Annotation `@AuditLog` kết hợp với AOP (Aspect-Oriented Programming) để tự động ghi lại lịch sử các thay đổi quan trọng (Ví dụ: Cập nhật tiến độ sản xuất, thay đổi trạng thái máy móc) mà không làm rác logic nghiệp vụ (Business logic).

### Tự Động Hóa Trạng Thái Trạm Máy
- Khi bắt đầu báo cáo sản lượng lớn hơn 0, Lệnh chuyển sang `IN_PROGRESS` và máy chuyển sang trạng thái `RUNNING`.
- Khi báo cáo sản lượng đạt mục tiêu, nếu cấu hình `AUTO_CLOSE_WO` được bật, hệ thống sẽ tự đóng lệnh và trả máy về trạng thái rảnh rỗi (`IDLE`).

---

## 🗄 Mô Hình Dữ Liệu (ERD Tóm Tắt)

```text
[Item Master] 1 --- N [Work Order] 1 --- N [Production Schedule] N --- 1 [Work Center]
      |                                               |
      1                                               1
      |                                               |
      N                                               N
    [BOM]                                     [Production Log] 1 --- 1 [Quality Check]
```

---

## 🚀 Hướng Dẫn Chạy Dự Án

### Yêu Cầu Cài Đặt
- JDK 17
- Maven 3.8+
- PostgreSQL 14+

### Cấu Hình Biến Môi Trường (Environment Variables)
Bạn cần thiết lập các biến sau (hoặc cấu hình trực tiếp trong `application.yml`):
```properties
POSTGRESQL_URL=jdbc:postgresql://localhost:5432/smartmes_db
POSTGRESQL_USER=postgres
POSTGRESQL_PASS=yourpassword
JWT_SECRET=your_base64_encoded_secret_key_here
DOMAIN=http://localhost:5173 # Origin của Frontend để setup CORS & Cookies
```

### Khởi Động
```bash
mvn clean install
mvn spring-boot:run
```
Hệ thống sẽ chạy ở port mặc định: `http://localhost:8080`

---

## 📡 WebSocket Endpoints

- **Endpoint kết nối:** `/ws-mes` (Sử dụng SockJS & STOMP)
- **Subscribe Topics:**
  - `/topic/dashboard`: Nhận các luồng sự kiện cập nhật chung.
  - `/topic/dashboard/progress`: Nhận payload chi tiết về % hoàn thành của từng máy sản xuất.
  - `/topic/alerts`: Kênh nhận cảnh báo khẩn cấp (máy hỏng, hàng lỗi).

---

## 📝 Tác Giả / Nhóm Phát Triển
*Đồ án tốt nghiệp / Dự án SmartMES*