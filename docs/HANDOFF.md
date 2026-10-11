# Bàn giao base cho nhóm

## Kiệt gửi những gì

1. Repo source: https://github.com/kiethk/car-rental-management-system — public.
2. README: Java 21, Docker/PostgreSQL, chạy ứng dụng, cấu hình local và xử lý xung đột cổng.
3. `database/schema.sql` + `database/seed.sql`: physical đề xuất 13 bảng, 2 loại xe, 3 xe, 3 biển số, 4 khung giờ tương lai.
4. `docs/reference/erd_car.drawio` + `docs/ERD-REVIEW.md`: conceptual 13 thực thể; SLOT là khung giờ thuê theo loại xe; physical trong file drawio còn 8 bảng.
5. `docs/TEAM-PLAN.md`: phần việc, phụ thuộc, ticket và điều kiện nghiệm thu.
6. UI tham khảo: https://github.com/kiethk/car-rental-management-system-ui — chuyển bố cục cần dùng sang Thymeleaf.

Bàn giao bằng repo + tài liệu; mỗi thành viên tạo database local riêng. Không gửi thư mục target, mật khẩu cá nhân hay volume Docker.

## Base đã có / chưa có

Đã có: cấu trúc controller/service/service-impl/repository/entity/dto, 13 entity + repository, PostgreSQL Compose, SQL seed, Bootstrap, danh sách/tìm kiếm/chi tiết xe, kiểm tra 404 và 5 test MVC, CI build.

Chưa có: auth/role/GPLX upload, CRUD đầy đủ, booking/capacity/assignment, xử lý cọc/hoàn/phạt, promotion/review/issue/inspection nghiệp vụ. Không coi entity hoặc template trống là chức năng đã hoàn thành.

## Bắt đầu trên máy mỗi người

1. Kiệt thêm collaborator; mọi người nhận lời mời để push nhánh.
2. Clone repo, mở project Maven bằng JDK 21.
3. Bật Docker Desktop; trong thư mục project chạy:

```bash
docker compose up -d
mvn verify
mvn spring-boot:run
```

4. Mở http://localhost:8081/vehicles; thấy ba xe và tìm kiếm Toyota/Honda được.
5. DBeaver: localhost:5434, database car_rental_management, user crms, password crms_password — thông tin demo local.
6. Nếu port app trùng, đặt SERVER_PORT hoặc file application-local.properties. Nếu port DB trùng, copy .env.example sang .env, chọn DB_PORT còn trống, đặt DB_URL tương ứng cho Spring. `.env` chỉ được Compose đọc.

Nếu chưa cài Maven, dùng `./mvnw` trên Git Bash hoặc `.\mvnw.cmd` trên PowerShell. SQL khởi tạo chỉ chạy lần đầu tạo volume. Không xóa volume để sửa lỗi đăng nhập.

Nghiệm thu base: cả bốn máy chạy được, `mvn verify` pass, gửi kết quả trong nhóm. Đây là mốc đầu; chưa yêu cầu chạy các chức năng chưa viết.

## Chốt người phụ trách và ticket đầu

| Người | Module sở hữu | Việc đầu tiên | Review |
|---|---|---|---|
| Kiệt | Slot, Booking, Payment; schema và tích hợp | SLOT-01: list/tạo slot theo loại xe, kiểm tra giờ/capacity/chồng lịch | Phúc review fleet, Phú review quyền |
| Phú | Customer, Employee, LicenseDocument; auth/quyền | AUTH-01: register/login/logout, hash password, phân biệt khách/staff/admin | Kiệt |
| Phúc | VehicleType, Vehicle, LicensePlate, VehicleInspection | FLEET-01: CRUD loại xe/xe/biển số, validate trùng và trạng thái | Kiệt |
| Trí | Promotion, Review, Issue | PROMO-01: danh sách → thêm → sửa khuyến mãi; validate ngày, code, percent | Phúc; Kiệt hướng dẫn mẫu |

Trí làm từng PR nhỏ; sau PROMO-01 mới chuyển Review rồi Issue khi booking đã sẵn sàng. Phú hỗ trợ quyền/ownership cho Review; Kiệt hỗ trợ liên kết Issue với Booking/Slot/Vehicle. Không giao Trí tự xử lý tiền hoặc concurrent booking.

Mỗi người viết cả DTO + service + controller + Thymeleaf + kiểm tra phần mình. Dùng mẫu VehicleService/VehicleController hiện có. Không chia nhóm chỉ frontend/backend.

## Trước khi mở rộng nghiệp vụ

Kiệt tổ chức một buổi ngắn chốt các điểm trong ERD-REVIEW:

- Base đang đề xuất biển số–xe hiện hành 1:1; conceptual đang vẽ 1:N.
- Booking được chưa gán xe lúc tạo; trước nhận xe phải có xe/biển số hợp lệ.
- Slot theo loại xe, có capacity, MVP đặt toàn slot; không cho slot cùng loại chồng nhau nếu chưa có thuật toán tổng tồn kho.
- Chính sách cọc bảo đảm, thanh toán cuối, hoàn khi trả xe/hủy, phí phạt.
- Auth dùng session/role, bảo vệ POST và CSRF; không cần JWT cho ứng dụng MVC này.

Schema là bản đề xuất có thể chạy, chưa phải bằng chứng các quy tắc nghiệp vụ đã được triển khai. Không tự đổi entity/schema trên nhánh riêng mà chưa phối hợp Kiệt.

## Quy trình làm việc

- Nhánh chức năng → PR vào main → một reviewer → Kiệt merge. Không push trực tiếp main khi làm feature.
- Nhánh đầu: feature/kiet-slot, feature/phu-auth, feature/phuc-fleet, feature/tri-promotion.
- PR cần: hành vi hoàn thành, ảnh trang nếu sửa UI, cách kiểm tra, thay schema nếu có.
- Chức năng xong khi dữ liệu lưu PostgreSQL, còn sau restart, có validate/kiểm tra quyền, không lỗi khi ID không tồn tại, `mvn verify` pass.
- Không mở rộng cổng thanh toán thật, bản đồ hoặc dashboard phức tạp trước khi có luồng thuê hoàn chỉnh.

Deadline chưa được cung cấp. Kế hoạch hiện theo mốc: setup → CRUD/auth/slot → luồng thuê → tích hợp/demo; xem TEAM-PLAN để chia theo buổi.
