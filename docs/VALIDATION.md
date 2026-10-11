# Kết quả kiểm tra

## Source UI tham khảo

- 17 test rentalLogic: pass.
- TypeScript `--noEmit`: pass.
- Source UI, ERD và project PE giữ nguyên.
- Chưa kiểm tra giao diện React trực quan trong trình duyệt.

## Bản base

Đã chạy trên JDK 21.0.12, Spring Boot 4.1.1, PostgreSQL 16.15 ngày 11/10/2026:

- Compile 31 file Java: pass.
- 5 test MVC/Thymeleaf: 5 pass, 0 fail, 0 error.
- Đóng gói executable JAR: pass.
- `docker compose config --quiet`: pass.
- PostgreSQL tạm riêng: schema tạo đủ 13 bảng, seed có 2 loại xe / 3 xe / 3 biển số / 4 slot.
- Khởi động ứng dụng: 13 repository được quét; Hibernate `ddl-auto=validate` kiểm tra mapping và khởi tạo JPA thành công.
- HTTP thật: `/` 200; `/vehicles?keyword=Honda` 200 và chỉ có Honda; tìm kiếm không có kết quả 200 với thông báo rỗng; `/vehicles/1` 200; `/vehicles/9999` 404.
- Sửa lỗi tìm thấy khi chạy thật: PostgreSQL từ chối alias `Asia/Saigon` do JVM trên Windows gửi lên. Application chuẩn hóa timezone JVM về UTC trước khi mở JDBC; thời gian nghiệp vụ dùng OffsetDateTime, giao diện lịch cần hiển thị Asia/Ho_Chi_Minh.
- Test dùng Mockito agent cấu hình sẵn để tránh Java tự attach trong môi trường hạn chế quyền.

PostgreSQL dùng container tạm riêng, cổng động, dữ liệu trong bộ nhớ; không dùng database/volume hiện có của người dùng. App thử nghiệm và container được dừng sau kiểm tra.

Nghiệp vụ booking/auth/payment, capacity đồng thời, upload và phân quyền chưa triển khai; không coi các phần này đã được kiểm tra. Chưa kiểm tra bố cục trang base trực quan trên trình duyệt.
