# CRMS — Car Rental Management System

Base nhóm HSF302: Kiệt, Phú, Phúc, Trí.

Repo nhóm: [kiethk/car-rental-management-system](https://github.com/kiethk/car-rental-management-system).
UI tham khảo: [car-rental-management-system-ui](https://github.com/kiethk/car-rental-management-system-ui).

## Phạm vi bản base

- Một ứng dụng Spring MVC + Thymeleaf, theo project PE đã tham khảo.
- Java 21, Maven, Spring Boot 4.1.1, Spring Data JPA/Hibernate, PostgreSQL 16, Bootstrap từ project PE.
- 13 entity + repository; schema SQL đề xuất theo **conceptual ERD**.
- Mẫu hoàn chỉnh: danh sách, tìm kiếm hãng/mẫu, chi tiết xe, trả 404 khi xe không tồn tại.
- Booking, tài khoản, phân quyền, GPLX, thanh toán, khuyến mãi, đánh giá, sự cố, kiểm định: chưa triển khai nghiệp vụ.
- React UI là mẫu bố cục và luồng nghiệp vụ; không chạy React trong base này.
- Các quyết định schema chưa được duyệt nằm trong [ERD review](docs/ERD-REVIEW.md).

## Chạy bằng database mới

Yêu cầu JDK 21+, Maven 3.6.3+, Docker Desktop đang chạy. Wrapper có sẵn nếu chưa cài Maven.

Chạy trong thư mục project:

```powershell
Copy-Item .env.example .env
docker compose up -d
mvn spring-boot:run
# Hoặc: .\mvnw.cmd spring-boot:run
```

Mở [danh sách xe](http://localhost:8081/vehicles). Có 2 loại xe, 3 xe, 3 biển số, 4 slot mẫu.

DBeaver: host `localhost`, port `5434`, database `car_rental_management`, user `crms`, password `crms_password` — chỉ là thông tin local mẫu.

Compose mới dùng volume riêng `crms-team_postgres_data`; không tái sử dụng volume cũ. Nếu port 5434 đang dùng, đổi `DB_PORT=5436` trong `.env`, rồi đặt URL Spring tương ứng trong file local bên dưới. Không cần dừng hoặc xóa database cũ.

## Dùng PostgreSQL đang có

1. Tạo database mới/empty trong DBeaver, ví dụ `car_rental_management_team`.
2. Chạy `database/schema.sql`, rồi `database/seed.sql`, trên database mới đó.
3. Copy `config/application-local.properties.example` tới `src/main/resources/application-local.properties`; sửa URL/user/password.
4. Chạy `mvn spring-boot:run`.

Spring đọc biến `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT` hoặc file local. **Spring không tự đọc `.env` của Compose.** Nếu thay user/password/port Compose, phải sửa cấu hình Spring tương ứng.

SQL init của Docker chỉ chạy khi volume mới được tạo. Đổi biến môi trường không đổi password của database đã tồn tại. Không dùng `docker compose down -v` để xử lý lỗi đăng nhập vì sẽ mất dữ liệu.

## Cấu trúc

```text
src/main/java/com/crms/mvc/
  CarRentalApplication.java
  controller/        HTTP, form, Model, tên template
  service/           Interface nghiệp vụ
  service/impl/      Xử lý nghiệp vụ, transaction, map DTO
  repository/        JpaRepository
  entity/            13 JPA entity dùng chung
  dto/               Dữ liệu cho view/form
src/main/resources/
  application.properties
  templates/
    fragments/ vehicles/ auth/ profile/ licenses/
    bookings/ admin/ promotions/ reviews/
  static/css/bootstrap.css
database/            schema.sql, seed.sql
docs/                Kiểm tra hiện trạng, phân công, ERD, quy trình GitHub
```

## Quy ước

- Controller gọi service; service gọi repository. Không đặt nghiệp vụ trong HTML.
- Constructor injection; DTO cho form, không bind request thẳng vào entity.
- Entity liên quan dùng lazy; map DTO trong transaction vì `open-in-view=false`.
- Schema database-first; Hibernate `validate`, không tự sửa database.
- Tiền: `BigDecimal`/`NUMERIC(14,2)`, VND. Thời gian: `OffsetDateTime`; JVM/JDBC dùng UTC, giao diện lịch cần hiển thị `Asia/Ho_Chi_Minh`.
- Mật khẩu phải hash khi làm auth; chưa có tài khoản admin mặc định. Base không có endpoint quản trị ghi dữ liệu.
- Mỗi người làm cả Java và Thymeleaf trong module mình phụ trách.

## Kiểm tra

```powershell
mvn verify
```

5 test MVC chạy không cần database, kiểm tra render Thymeleaf, kết quả tìm kiếm, rỗng, escape input, chi tiết và 404. Khi thêm nghiệp vụ, bổ sung kiểm tra đúng phần thay đổi.

Bắt đầu tại [checklist bàn giao](docs/HANDOFF.md).

Xem [kết quả kiểm tra thực tế](docs/VALIDATION.md), [phân công](docs/TEAM-PLAN.md), [đưa lên GitHub](docs/GITHUB.md).

## Nguồn kỹ thuật

- [Spring Boot: Java/Maven requirements](https://docs.spring.io/spring-boot/system-requirements.html).
- [Spring Boot: database initialization và ddl-auto](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

Version Boot/Java giữ cùng project PE; PostgreSQL giữ lựa chọn đã dùng trước đó. Bootstrap giữ nguyên header MIT của file tham khảo.
