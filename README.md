# Identity Service - Hệ Thống Microservices (Session 13)

Dự án triển khai **Identity Service** ("Phòng quản lý nhân khẩu" trong hệ thống Microservices), đảm nhiệm việc lưu trữ thông tin đăng nhập và quản lý tài khoản người dùng an toàn với mã hóa BCrypt.

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
    ├── build.gradle                # Khai báo dependencies Spring Boot, Security, JPA, PostgreSQL
    └── src/
        ├── main/
        │   ├── java/com/example/identityservice/
        │   │   ├── IdentityServiceApplication.java
        │   │   ├── config/
        │   │   │   └── SecurityConfig.java         # Cấu hình SecurityFilterChain & BCryptPasswordEncoder
        │   │   ├── controller/
        │   │   │   └── AuthController.java         # API POST /api/auth/register
        │   │   ├── dto/
        │   │   │   ├── RegisterRequest.java        # Request validation (@NotBlank, @Size)
        │   │   │   └── UserResponse.java           # Response không chứa password
        │   │   ├── entity/
        │   │   │   └── User.java                   # Entity User (id UUID, username, password, role)
        │   │   ├── exception/
        │   │   │   ├── DuplicateResourceException.java
        │   │   │   └── GlobalExceptionHandler.java # Xử lý lỗi trùng username và validation
        │   │   ├── repository/
        │   │   │   └── UserRepository.java
        │   │   └── service/
        │   │       ├── UserService.java
        │   │       └── impl/UserServiceImpl.java   # Băm mật khẩu bằng BCrypt trước khi lưu
        │   └── resources/
        │       └── application.yml                 # Cấu hình kết nối PostgreSQL (identity_db)
        └── test/
            └── java/com/example/identityservice/
                ├── IdentityServiceApplicationTests.java
                ├── controller/
                │   ├── AuthControllerTest.java
                │   └── RegisterIntegrationTest.java# Test luồng đăng ký & kiểm tra băm mật khẩu trong DB
                └── service/
                    └── UserServiceTest.java
```

---

## 2. Thư Viện (Dependencies)

Trong [identity-service/build.gradle](identity-service/build.gradle):

- `org.springframework.boot:spring-boot-starter-data-jpa`: Tương tác cơ sở dữ liệu qua Hibernate/JPA.
- `org.springframework.boot:spring-boot-starter-security`: Bảo mật và mã hóa BCrypt.
- `org.springframework.boot:spring-boot-starter-web`: Xây dựng REST API.
- `org.springframework.boot:spring-boot-starter-validation`: Validate dữ liệu đầu vào.
- `org.postgresql:postgresql` (runtimeOnly): Driver kết nối cơ sở dữ liệu PostgreSQL.

---

## 3. Cơ Sở Dữ Liệu: PostgreSQL

- **Tên Database**: `identity_db`
- **Tên Bảng**: `users`
- **Các trường**:
  - `id` (VARCHAR(255), Primary Key): Định danh UUID tự sinh.
  - `username` (VARCHAR(255), Unique, Not Null): Tên đăng nhập.
  - `password` (VARCHAR(255), Not Null): Mật khẩu đã băm bằng **BCrypt** (`$2a$10$...`).
  - `role` (VARCHAR(255), Not Null): Phân quyền người dùng (mặc định: `USER`).

Khởi chạy PostgreSQL bằng Docker (nếu chưa cài PostgreSQL cục bộ):
```bash
docker compose up -d
```

---

## 4. Chi Tiết Triển Khai Kỹ Thuật

### 4.1. Cấu hình Spring Security (`SecurityConfig.java`)
- **Tắt CSRF**: `.csrf(AbstractHttpConfigurer::disable)`
- **PermitAll cho Endpoint đăng ký**: `.requestMatchers("/api/auth/register", "/api/auth/**").permitAll()`
- **BCrypt Password Encoder**:
  ```java
  @Bean
  public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
  }
  ```

### 4.2. API Đăng Ký Tài Khoản (`POST /api/auth/register`)
- **URL**: `http://localhost:8080/api/auth/register`
- **Method**: `POST`
- **Headers**: `Content-Type: application/json`

#### Request Body mẫu:
```json
{
  "username": "user_demo",
  "password": "Password123@",
  "role": "USER"
}
```

#### Response (HTTP 201 Created):
> **Lưu ý**: Response trả về thông tin user nhưng **hoàn toàn loại bỏ trường password** theo đúng yêu cầu bảo mật.
```json
{
  "id": "e44c2045-8fe3-44ce-b489-bb011559ee2d",
  "username": "user_demo",
  "role": "USER"
}
```

---

## 5. Hướng Dẫn Chạy & Kiểm Thử

### Chạy Service:
```bash
# Sử dụng Gradle Wrapper từ thư mục gốc
.\gradlew.bat :identity-service:bootRun
```

### Chạy Bộ Test Tự Động (Tất cả test đều đã PASS 100%):
```bash
.\gradlew.bat :identity-service:test
```

### Kiểm tra mật khẩu trong Database PostgreSQL:
```sql
SELECT id, username, password, role FROM users;
```
Kết quả hiển thị:
```
                  id                  |  username   |                           password                           | role 
--------------------------------------+-------------+--------------------------------------------------------------+------
 e44c2045-8fe3-44ce-b489-bb011559ee2d | user_demo   | $2a$10$wK1eO79Gk4wPzJdM0gZzO.e0bMsnFsmB03OaIqm9g58j7bJ12qHw6 | USER
```
=> Mật khẩu được mã hóa an toàn dạng chuỗi băm BCrypt (`$2a$10$...`), không lộ mật khẩu gốc.

