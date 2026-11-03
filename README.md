# CRMS — Car Rental Management System

Dự án HSF302 của Kiệt, Phú, Phúc, Trí. Java 21, Spring Boot 4.1.1, MVC + Thymeleaf + JPA, PostgreSQL 16, Bootstrap.

[UI tham khảo](https://github.com/kiethk/car-rental-management-system-ui): dùng bố cục/luồng để viết Thymeleaf.

## Chạy project

Cài JDK 21, Maven, PostgreSQL 16 và DBeaver.

1. Tạo database `car_rental_management` trên PostgreSQL local.
2. Mở DBeaver chạy lần lượt 2 file script:
   - `database/schema.sql` (tạo bảng)
   - `database/seed.sql` (dữ liệu mẫu)
3. Chạy ứng dụng:

```bash
git clone https://github.com/kiethk/car-rental-management-system.git
cd car-rental-management-system
mvn spring-boot:run
```

Mở http://localhost:8081/vehicles. Chưa cài Maven thì dùng `./mvnw spring-boot:run` trong Git Bash hoặc `.\mvnw.cmd spring-boot:run` trong PowerShell.

DBeaver: host `localhost`, port `5434` (hoặc `5432`), database `car_rental_management`, user `postgres`, password đặt theo máy local.

- App trùng cổng: đặt `SERVER_PORT` hoặc sửa `server.port` trong file local.
- Cấu hình riêng: copy [file mẫu](config/application-local.properties.example) thành `src/main/resources/application-local.properties`. File này không đưa lên Git.

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

## Hợp đồng thuê xe

- Điều khoản nằm trong bảng `contract_templates`, mỗi dòng là một phiên bản. Chỉ bản DRAFT được sửa; admin vào **Rental Terms** (`/admin/contract-templates`) để tạo bản nháp rồi kích hoạt. Tỉ lệ cọc, phí trễ và số giờ hủy miễn phí là các cột riêng, code đọc từ đúng phiên bản khách đã đồng ý.
- Khi checkout, khách tick đồng ý: hệ thống lưu thời điểm, IP và phiên bản vào `contracts`. Sau khi cọc VNPay thành công, `ContractIssuingListener` sinh PDF (`templates/pdf/rental-contract.html`) vào `uploads/contracts/` (không public) và gửi email kèm PDF.
- Gửi email hợp đồng là **tùy chọn**. Không cấu hình thì app vẫn chạy bình thường, chỉ ghi log "Email is not configured" và admin có thể gửi lại sau. Muốn gửi thật qua Gmail:
  1. Bật xác minh 2 bước cho tài khoản Google, tạo App Password tại https://myaccount.google.com/apppasswords.
  2. Thêm vào `application-local.properties` (xem [file mẫu](config/application-local.properties.example)): `spring.mail.username=<gmail>` và `spring.mail.password=<app password 16 ký tự>`.
  3. Khi test, dùng email của chính mình cho tài khoản khách (ví dụ `ten.ban+khach1@gmail.com`); dữ liệu seed dùng `@example.com` nên không gửi tới ai.
- Schema đã đổi (bỏ `policies`, thêm `contract_templates`). DB tạo trước thay đổi này phải tạo lại: xóa database `car_rental_management`, tạo mới rồi chạy lại `schema.sql` + `seed.sql`. App dùng `ddl-auto=validate` nên sẽ báo lỗi ngay khi khởi động nếu schema DB không khớp entity.
- Câu chữ của các chính sách (cọc, hủy, trả trễ) viết trực tiếp trong từng trang, chỉ con số lấy từ DB. Mọi chỗ hiển thị chính sách đều gắn thuộc tính `data-policy` (`deposit`, `cancellation`, `lateReturn`); khi đổi quy tắc, tìm `data-policy` trong toàn project (Ctrl+Shift+F) để sửa đủ. Thêm chỗ hiển thị chính sách mới thì nhớ gắn thuộc tính này.
- ERD đề xuất cho phần này: [contract-templates-erd.md](docs/proposed/contract-templates-erd.md).

Kiểm tra build/test: `mvn verify`.
