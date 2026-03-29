# SmartMES - Task Implementation Summary

Tài liệu này mô tả chi tiết 3 task đã được thực hiện trên SmartMES backend (Java 17, Spring Boot 3.4.x).

---

## 🔐 TASK 1: Chuyển đổi JWT từ JSON Body sang HttpOnly Cookie

### Mục tiêu
- JWT token không còn được trả về trong JSON response
- Thay vào đó, token được lưu trong `HttpOnly` cookie (bảo mật cao, tránh XSS)
- Token được tự động gửi kèm mỗi request nếu domain khớp

### Files được sửa

#### 1. `src/main/java/com/smartmes/backend/modules/auth/AuthController.java`
**Thay đổi:**
- Thêm parameter `HttpServletResponse response` vào method `login()`
- Gọi `jwtService.setAuthCookie(response, token)` để thiết lập cookie
- Đổi `LoginResponse` từ trả `token` sang chỉ trả `fullName` và `role`
- **Thêm endpoint mới:** `POST /api/v1/auth/logout` để clear cookie

**Ví dụ login response trước:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "fullName": "Nguyễn Chương Hào",
  "role": "ROLE_ADMIN"
}
```

**Ví dụ login response sau:**
```json
{
  "fullName": "Nguyễn Chương Hào",
  "role": "ROLE_ADMIN"
}
// + Set-Cookie header: auth_token=...; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=...
```

#### 2. `src/main/java/com/smartmes/backend/modules/auth/JwtService.java`
**Thay đổi:**
- Thêm constants: `AUTH_COOKIE_NAME = "auth_token"` và `TOKEN_EXPIRATION_MS = 86400000` (24h)
- **Thêm method:** `setAuthCookie(HttpServletResponse response, String token)`
  - Thiết lập cookie với cờ: `HttpOnly=true`, `Secure=true`, `Path=/`, `SameSite=Lax`
- **Thêm method:** `clearAuthCookie(HttpServletResponse response)` (dùng cho logout)
- **Thêm method:** `getAuthCookieName()` (trả về tên cookie)

#### 3. `src/main/java/com/smartmes/backend/modules/auth/JwtAuthFilter.java`
**Thay đổi:**
- Đổi từ đọc token trong header `Authorization: Bearer <token>` sang đọc từ `HttpServletRequest.getCookies()`
- **Thêm private method:** `extractJwtFromCookie(HttpServletRequest request)` 
- Wrap logic trong try-catch để bắt `ExpiredJwtException`
- Thêm logging để debug

**Mã quản lý cookie:**
```java
private String extractJwtFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (jwtService.getAuthCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
    }
    return null;
}
```

#### 4. `src/main/java/com/smartmes/backend/core/config/CorsConfig.java`
**Trạng thái:** ✅ Không cần thay đổi (đã có `allowCredentials(true)`)
- CORS config đã hỗ trợ credentials (cookies)
- Origin cụ thể `http://localhost:5173` được cấu hình (không dùng wildcard `*`)

**Lưu ý:** Để sử dụng cookies với CORS, frontend **bắt buộc** phải:
- Gửi request với `credentials: 'include'` (fetch API) hoặc `withCredentials: true` (axios)
- Origin của frontend phải khớp với `allowedOrigins`

---

## 🛑 TASK 2: Xử lý JWT Token Hết Hạn (Expired Token)

### Mục tiêu
- Bắt lỗi `ExpiredJwtException` và trả về HTTP 401 (Unauthorized) rõ ràng
- Tránh lỗi 500 Internal Server Error hoặc 403 CORS khi token hết hạn
- Xóa cookie hết hạn khỏi client

### Files được sửa

#### 1. `src/main/java/com/smartmes/backend/modules/auth/JwtAuthFilter.java`
**Thay đổi:**
- Try-catch block bắt `ExpiredJwtException`
- Khi token hết hạn:
  - Xóa cookie khỏi response: `jwtService.clearAuthCookie(response)`
  - Thiết lập HTTP status 401
  - Ghi JSON error response và return (không tiếp tục filter chain)
  - Log warning cho debug

**Code:**
```java
try {
    // ... logic xác thực
} catch (ExpiredJwtException ex) {
    logger.warn("JWT token has expired: {}", ex.getMessage());
    jwtService.clearAuthCookie(response);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("{...error message...}");
    return;
}
```

#### 2. `src/main/java/com/smartmes/backend/core/exception/GlobalExceptionHandler.java`
**Thay đổi:**
- **Thêm handler mới:** `handleExpiredJwtException(ExpiredJwtException ex)`
  - Trả về HTTP 401 + message "JWT token has expired. Please login again."
- **Thêm handler mới:** `handleAuthenticationException(AuthenticationException ex)`
  - Bắt Spring Security authentication errors
  - Trả về HTTP 401
- Sắp xếp order handlers: JWT exceptions xử lý trước generic RuntimeException

**Ưu điểm:**
- `JwtAuthFilter` catch sớm, trả response 401 ngay tức khắc
- `GlobalExceptionHandler` là backup nếu exception vượt qua filter
- Frontend luôn nhận 401 thay vì 500 hoặc lỗi CORS

---

## 📊 TASK 3: Đồng bộ Trạng thái Máy lên Dashboard Realtime

### Mục tiêu
- Khi trạng thái máy thay đổi (`DOWN`, `IDLE`, `OFFLINE`, `MAINTENANCE`, `RUNNING`)
- Phát sự kiện realtime tới topic `/topic/dashboard` via WebSocket
- Frontend subscribe `/topic/dashboard` để tự động refetch dữ liệu dashboard

### Files được sửa

#### 1. `src/main/java/com/smartmes/backend/modules/masterdata/service/WorkCenterService.java`
**Thay đổi:**
- **Inject dependency:** `SimpMessagingTemplate messagingTemplate`
- Trong **3 methods** xử lý trạng thái máy:
  1. `reportMachineDown()` - báo máy hỏng
  2. `resolveMachineIssue()` - sửa xong máy
  3. `updatePing()` - nhận heartbeat từ IoT device
- Thêm call `publishMachineStatusUpdate(workCenter)` sau khi cập nhật DB

#### 2. Private method mới: `publishMachineStatusUpdate()`
**Công dụng:** Phát event tới WebSocket topic

**Event structure:**
```json
{
  "event": "MACHINE_STATUS_CHANGED",
  "workCenterId": 1,
  "workCenterCode": "CNC-01",
  "workCenterName": "CNC Milling Machine 01",
  "status": "DOWN",
  "timestamp": "2026-03-29T..."
}
```

**Code:**
```java
private void publishMachineStatusUpdate(WorkCenter workCenter) {
    Map<String, Object> statusUpdate = new HashMap<>();
    statusUpdate.put("event", "MACHINE_STATUS_CHANGED");
    statusUpdate.put("workCenterId", workCenter.getId());
    statusUpdate.put("workCenterCode", workCenter.getCode());
    statusUpdate.put("workCenterName", workCenter.getName());
    statusUpdate.put("status", workCenter.getCurrentStatus().name());
    statusUpdate.put("timestamp", LocalDateTime.now());
    
    messagingTemplate.convertAndSend("/topic/dashboard", statusUpdate);
}
```

### Kết quả

| Action (API) | Alert Topic | Dashboard Topic |
|---|---|---|
| `POST /work-centers/{id}/down` | ✅ `/topic/alerts` (MACHINE_DOWN) | ✅ `/topic/dashboard` |
| `PATCH /work-centers/{id}/resolve` | ✅ `/topic/alerts` (MACHINE_FIXED) | ✅ `/topic/dashboard` |
| `PATCH /work-centers/{id}/ping` | ✅ `/topic/alerts` (nếu recovery) | ✅ `/topic/dashboard` |

**Frontend side (ví dụ):**
```javascript
const stompClient = new Stomp.client('ws://localhost:8080/ws-mes');

stompClient.connect({}, () => {
    // Subscribe dashboard updates
    stompClient.subscribe('/topic/dashboard', (message) => {
        const event = JSON.parse(message.body);
        if (event.event === 'MACHINE_STATUS_CHANGED') {
            // Refetch dashboard stats hoặc update UI với status mới
            console.log(`Machine ${event.workCenterName} is now ${event.status}`);
            refreshDashboard();
        }
    });
});
```

---

## 🧪 Testing Checklist

### Test Task 1 (Cookie-based JWT)
- [ ] `POST /api/v1/auth/login` with valid credentials
  - Response không chứa `token` trong JSON
  - Response header có `Set-Cookie: auth_token=...`
  - Cookie có flag `HttpOnly; Secure; SameSite=Lax`
  
- [ ] Call API yêu cầu auth (e.g., `GET /api/v1/dashboard/stats`)
  - Browser tự động gửi cookie `auth_token`
  - API accept request nếu token valid
  
- [ ] Test CORS with credentials
  - Frontend: `fetch(url, { credentials: 'include' })`
  - Response header có `Access-Control-Allow-Credentials: true`

### Test Task 2 (Expired JWT)
- [ ] Clear cookies, set token manually với expiration time sắp hết
  - Thực hiện yêu cầu API
  - Response status: **401 Unauthorized**
  - Response body: `{"message": "JWT token has expired. Please login again."}`
  - Cookie `auth_token` bị xóa (Max-Age=0)

- [ ] Call after logout
  - Response status: **401 Unauthorized**

### Test Task 3 (Dashboard Sync)
- [ ] WebSocket client connect tới `/ws-mes`
- [ ] Subscribe `/topic/dashboard`
- [ ] Call machine status change API: `POST /work-centers/1/down?reason=test`
  - WebSocket nhận message: 
    ```json
    {"event": "MACHINE_STATUS_CHANGED", "status": "DOWN", ...}
    ```
- [ ] Verify `/topic/alerts` cũng nhận được alert
- [ ] Dashboard thực hiện refetch stats

---

## ⚠️ Important Notes

### Security Considerations
1. **HttpOnly Cookie:**
   - JavaScript không thể access cookie (bảo vệ vs XSS attack)
   - Token auto-attach mỗi request (đơn giản hơn manual Authorization header)
   - Bắt buộc: `Secure` flag+ HTTPS in production

2. **CORS với Credentials:**
   - Frontend **KHÔNG** dùng `allowedOrigins: '*'`
   - Frontend **PHẢI** gửi `credentials: 'include'`
   - Domain phải khớp chính xác

3. **Expired Token Handling:**
   - Client sẽ nhận 401 khi token hết hạn
   - Client nên redirect tới login page
   - Cookie hết hạn được xóa server-side

### Database/Config Requirements
- Không cần thay đổi database schema
- Environment variables giữ nguyên:
  - `JWT_SECRET` (Base64 encoded)
  - `DOMAIN` (frontend origin)
  - `POSTGRESQL_*` params

### Backward Compatibility
- ⚠️ **Frontend phải cập nhật:**
  - Đổi từ header-based auth sang cookie-based auth
  - Enable `credentials: 'include'` trong fetch/axios
  - Xử lý 401 response (redirect tới login)

- ✅ **Backend API endpoints không thay đổi** (chỉ cơ chế auth thay đổi)

---

## 📝 Summary of Code Changes

| File | Changes | Risk |
|---|---|---|
| `AuthController.java` | Added HttpServletResponse param, setAuthCookie(), logout endpoint | Low |
| `JwtService.java` | Added setAuthCookie(), clearAuthCookie(), getAuthCookieName() | Low |
| `JwtAuthFilter.java` | Read from cookie, catch ExpiredJwtException, early response | Medium (auth critical) |
| `GlobalExceptionHandler.java` | Added ExpiredJwtException handler | Low |
| `WorkCenterService.java` | Injected SimpMessagingTemplate, call publishMachineStatusUpdate() | Low |

**Type of changes:** Feature enhancement + Security improvement

---

Generated: 2026-03-29  
Project: SmartMES Backend (Java 17, Spring Boot 3.4.x)
