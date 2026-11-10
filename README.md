# Identity Service - Hệ Thống Microservices (Session 13)

Dự án triển khai **Identity Service** ("Phòng quản lý nhân khẩu" trong hệ thống Microservices), đảm nhiệm hai vai trò cốt lõi:
1. **Bài 1**: Đăng ký và lưu trữ tài khoản người dùng an toàn với mật khẩu băm BCrypt trong PostgreSQL.
2. **Bài 2**: Phát hành "tấm thẻ" (JWT Token) chứa thông tin `sub` (username) và `role` để người dùng xác thực với các service khác trong hệ thống Microservices.

---

## 1. Cấu Trúc Dự Án

```
ex_ss13_micro/
├── .gitignore                      # Chỉ commit source code & cấu hình cần thiết
├── docker-compose.yml              # Khởi chạy PostgreSQL (identity_db) độc lập
├── settings.gradle                 # Quản lý module identity-service
├── gradlew / gradlew.bat           # Gradle Wrapper
├── requests.http                   # File test API sẵn sàng cho VS Code / IntelliJ
├── README.md                       # Tài liệu hướng dẫn chi tiết
└── identity-service/
    ├── build.gradle                # Khai báo dependencies Spring Boot, Security, JPA, JJWT, PostgreSQL
    └── src/
        ├── main/
        │   ├── java/com/example/identityservice/
        │   │   ├── IdentityServiceApplication.java
        │   │   ├── config/
        │   │   │   └── SecurityConfig.java         # Cấu hình SecurityFilterChain & BCryptPasswordEncoder
        │   │   ├── controller/
        │   │   │   └── AuthController.java         # POST /api/auth/register & GET /api/auth/test-token
        │   │   ├── dto/
        │   │   │   ├── RegisterRequest.java        # Request validation (@NotBlank, @Size)
        │   │   │   └── UserResponse.java           # Response không chứa password
        │   │   ├── entity/
        │   │   │   └── User.java                   # Entity User (id UUID, username, password, role)
        │   │   ├── exception/
        │   │   │   ├── DuplicateResourceException.java
        │   │   │   ├── ResourceNotFoundException.java
        │   │   │   └── GlobalExceptionHandler.java # Xử lý lỗi trùng username, validation, 404
        │   │   ├── repository/
        │   │   │   └── UserRepository.java
        │   │   ├── service/
        │   │   │   ├── UserService.java
        │   │   │   └── impl/UserServiceImpl.java   # Xử lý logic đăng ký & tạo token từ DB
        │   │   └── util/
        │   │       └── JwtUtil.java                # Tạo & giải mã JWT Token (HS256, claim sub, claim role)
        │   └── resources/
        │       └── application.yml                 # Cấu hình PostgreSQL (identity_db) & jwt-secret / expiration
        └── test/
            └── java/com/example/identityservice/
                ├── IdentityServiceApplicationTests.java
                ├── controller/
                │   ├── AuthControllerTest.java     # Test controller endpoints
                │   └── RegisterIntegrationTest.java# Test End-to-End đăng ký & tạo token từ DB
                ├── service/
                │   └── UserServiceTest.java
                └── util/
                    └── JwtUtilTest.java            # Test sinh token, claim sub, custom claim role, TTL
```

---

## 2. Thư Viện (Dependencies)

Trong [identity-service/build.gradle](identity-service/build.gradle):

- `org.springframework.boot:spring-boot-starter-data-jpa`: Tương tác cơ sở dữ liệu qua Hibernate/JPA.
- `org.springframework.boot:spring-boot-starter-security`: Bảo mật và mã hóa BCrypt.
- `org.springframework.boot:spring-boot-starter-web`: Xây dựng REST API.
- `org.springframework.boot:spring-boot-starter-validation`: Validate dữ liệu đầu vào.
- `io.jsonwebtoken:jjwt-api:0.11.5`: Thư viện tạo và xử lý JSON Web Token (JWT).
- `io.jsonwebtoken:jjwt-impl:0.11.5` & `io.jsonwebtoken:jjwt-jackson:0.11.5`: Runtime implementation và JSON Jackson parser cho JJWT.
- `org.postgresql:postgresql` (runtimeOnly): Driver kết nối PostgreSQL.

---

## 3. Cấu Hình Hệ Thống (`application.yml`)

```yaml
server:
  port: 8080

spring:
  application:
    name: identity-service
  datasource:
    url: jdbc:postgresql://localhost:5432/identity_db
    username: postgres
    password: ${POSTGRES_PASSWORD:17111998}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970 # 256-bit secret key
  expiration: 3600000 # 3600000ms = 1 giờ
```

---

## 4. Chi Tiết Triển Khai Kỹ Thuật

### 4.1. Lớp `JwtUtil` (`generateToken(User user)`)
- Sử dụng thuật toán ký `SignatureAlgorithm.HS256` cùng khóa bí mật 256-bit.
- **Payload**:
  - `sub`: `user.getUsername()` (tiêu chuẩn subject claim).
  - `role`: `user.getRole()` (custom claim).
  - `iat`: Thời điểm phát hành token.
  - `exp`: Thời điểm hết hạn token (`now + 3600000ms`).

### 4.2. API Đăng Ký Tài Khoản (`POST /api/auth/register`)
- **URL**: `http://localhost:8080/api/auth/register`
- Băm mật khẩu bằng BCrypt (`$2a$10$...`) trước khi lưu vào PostgreSQL.
- Trả về thông tin User (id, username, role), **hoàn toàn loại bỏ password**.

### 4.3. API Cấp JWT Token Thử Nghiệm (`GET /api/auth/test-token?username={name}`)
- **URL**: `http://localhost:8080/api/auth/test-token?username=user_demo`
- Tìm người dùng trong cơ sở dữ liệu `identity_db`.
- Nếu tìm thấy: Dùng `JwtUtil` tạo và trả về chuỗi JWT.
- Nếu không tìm thấy: Trả về lỗi `404 Not Found`.

---

## 5. Hướng Dẫn Chạy & Kiểm Thử

### Khởi chạy ứng dụng:
```powershell
.\gradlew.bat :identity-service:bootRun
```

### Chạy toàn bộ Test tự động (100% PASS):
```powershell
.\gradlew.bat :identity-service:test
```

### Kiểm tra JWT trên `jwt.io`:
Khi dán chuỗi token nhận được từ API `GET /api/auth/test-token?username=user_demo` vào trang [jwt.io](https://jwt.io), phần Payload sẽ hiển thị:
```json
{
  "sub": "user_demo",
  "role": "USER",
  "iat": 1731248000,
  "exp": 1731251600
}
```
