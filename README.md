# CRMS — Car Rental Management System

Dự án HSF302 của Kiệt, Phú, Phúc, Trí. Java 21, Spring Boot 4.1.1, MVC + Thymeleaf + JPA, PostgreSQL 16, Bootstrap.

[UI tham khảo](https://github.com/kiethk/car-rental-management-system-ui): dùng bố cục/luồng để viết Thymeleaf.

## Chạy project

Cài JDK 21, Maven, Docker Desktop; bật Docker trước khi chạy.

```bash
git clone https://github.com/kiethk/car-rental-management-system.git
cd car-rental-management-system
docker compose up -d
mvn spring-boot:run
```

Mở http://localhost:8081/vehicles. Chưa cài Maven thì dùng `./mvnw spring-boot:run` trong Git Bash hoặc `.\mvnw.cmd spring-boot:run` trong PowerShell.

DBeaver: host `localhost`, port `5434`, database `car_rental_management`, user `crms`, password `crms_password` — cấu hình demo local.

- App trùng cổng: đặt `SERVER_PORT` hoặc sửa `server.port` trong file local.
- DB trùng cổng: copy `.env.example` thành `.env`, chọn `DB_PORT` còn trống; sửa URL Spring tương ứng.
- Cấu hình riêng: copy [file mẫu](config/application-local.properties.example) thành `src/main/resources/application-local.properties`. File này không đưa lên Git.
- `.env` chỉ được Docker Compose đọc. Nếu đổi user/password/port DB, cập nhật cấu hình Spring tương ứng.
- SQL mẫu tự chạy khi tạo volume lần đầu. Không xóa volume để sửa lỗi kết nối vì sẽ mất dữ liệu.

## Phân luồng

| Người | Luồng phụ trách |
|---|---|
| Kiệt | Chọn loại xe/slot → đặt xe → cọc → gán xe → nhận/trả xe → thanh toán cuối; hủy/hoàn tiền; ghép các phần |
| Phú | Đăng ký/đăng nhập → hồ sơ → nộp GPLX → nhân viên duyệt/từ chối; tài khoản khách/nhân viên và phân quyền |
| Phúc | Quản lý loại xe → xe → biển số → kiểm định/bảo trì; danh sách, tìm kiếm, chi tiết xe |
| Trí | Quản lý khuyến mãi; khách đánh giá booking đã hoàn thành; ghi nhận và cập nhật sự cố |

Trí bắt đầu từ khuyến mãi, sau đó đánh giá và sự cố; Kiệt/Phúc hỗ trợ khi cần. Mỗi người làm cả xử lý Java và trang Thymeleaf trong luồng của mình.

Nhắn nhóm tiến độ/vướng mắc; cần trao đổi thêm thì nói trên lớp. Khi đổi entity/schema hoặc đụng phần người khác, nhắn nhóm trước. Mỗi người làm nhánh riêng, xong push và báo Kiệt ghép.

## Base hiện có

- 13 entity/repository, kết nối PostgreSQL, SQL tạo bảng và dữ liệu mẫu.
- Danh sách, tìm kiếm, chi tiết xe và xử lý xe không tồn tại.
- Các luồng tài khoản, booking, tiền, khuyến mãi, đánh giá, kiểm định, sự cố còn cần triển khai.

## Code và database

- Java: `src/main/java/com/crms/mvc/`; theo `controller → service → service/impl → repository → entity`, DTO cho form/view.
- Giao diện: `src/main/resources/templates/`; CSS trong `static/`.
- SQL: [schema](database/schema.sql), [seed](database/seed.sql). Nếu dùng DB sẵn, chạy trên database mới/empty rồi chỉnh cấu hình local.
- ERD: [erd_car.drawio](docs/reference/erd_car.drawio). Conceptual có 13 thực thể; physical trong file còn 8 bảng. `SLOT` là khung giờ thuê theo loại xe.
- Schema base là đề xuất: biển số–xe hiện hành 1:1, booking được gán xe sau; cần thống nhất capacity slot và chính sách cọc/hoàn/phạt khi làm luồng thuê.
- Mẫu để làm theo: `VehicleController`, `VehicleService`, `VehicleServiceImpl`, `VehicleDTO`, các trang `templates/vehicles/`.

Kiểm tra build/test: `mvn verify`.
