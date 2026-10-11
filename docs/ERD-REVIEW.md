# Conceptual ERD và mapping đề xuất

**Nguồn thiết kế hiện tại: conceptual 13 thực thể.** Physical 8 bảng và Mermaid nhúng chưa được đồng bộ. File `reference/erd_car.drawio` là bản sao nguyên trạng; không chỉnh file gốc.

## Quan hệ đang vẽ

| Phía 1 | Phía 2 | Cách đọc trên sơ đồ |
|---|---|---|
| CUSTOMER | BOOKING | 1 khách — 0..N booking; mỗi booking có 1 khách |
| VEHICLE | BOOKING | 1 xe — 0..N booking; mỗi booking có 1 xe |
| EMPLOYEE | BOOKING | 1 nhân viên — 0..N booking; booking có 0..1 nhân viên |
| PROMOTION | BOOKING | 1 khuyến mãi — 0..N booking; booking có 0..1 khuyến mãi |
| BOOKING | PAYMENT | 1 booking — 0..N thanh toán |
| BOOKING | REVIEW | 1 booking — 0..1 đánh giá |
| CUSTOMER | LICENSE_DOCUMENT | 1 khách — 0..N hồ sơ GPLX |
| EMPLOYEE | LICENSE_DOCUMENT | 1 nhân viên — 0..N hồ sơ; hồ sơ có 0..1 người duyệt |
| VEHICLE_TYPE | VEHICLE | 1 loại — 0..N xe; xe thuộc 1 loại |
| VEHICLE_TYPE | SLOT | 1 loại — 0..N khung giờ; slot thuộc 1 loại |
| EMPLOYEE | ISSUE | 1 nhân viên — 0..N sự cố |
| VEHICLE | ISSUE | 1 xe — 0..N sự cố |
| SLOT | ISSUE | 1 slot — 0..N sự cố |
| SLOT | BOOKING | 1 slot — 0..N booking; booking thuộc 1 slot |
| VEHICLE | VEHICLE_INSPECTION | 1 xe — 0..N kiểm định |
| BOOKING | ISSUE | 1 booking — 0..N sự cố |
| LICENSE_PLATE | VEHICLE | Đang vẽ 1 biển số — 0..N xe |

Quan hệ được đọc từ `mxCell.source/target` và `startArrow/endArrow`, không theo metadata Mermaid cũ.

## Mapping trong base — đề xuất để review

Conceptual không có đủ thuộc tính/kiểu dữ liệu cho 5 thực thể mới. SQL/JPA trong base là **bản physical đề xuất**, chưa phải schema đã được người dùng duyệt.

| Thực thể | Thuộc tính chính đề xuất | Ghi chú |
|---|---|---|
| Customer | name, phone, email, password_hash, created_at | Email unique, password hash khi làm auth |
| Employee | name, phone, email, password_hash, role | Thêm đăng nhập staff/admin, role admin/staff |
| VehicleType | name, brand, category, seats, transmission, fuel_type, price_per_hour, deposit_amount | Thông tin/giá theo loại xe như UI |
| LicensePlate | plate, province, registration_expiry | Biển số hiện hành |
| Vehicle | vehicle_type_id, license_plate_id, color, year, odometer, status | Xe vật lý |
| Slot | vehicle_type_id, start_time, end_time, capacity, status | Khung giờ theo loại; capacity là đề xuất hỗ trợ nhiều booking |
| Booking | customer_id, slot_id, vehicle_id, employee_id, promotion_id, giờ nhận/trả, giá snapshot, cọc/giảm/hoàn, trạng thái | Giữ trường giao/nhận từ physical cũ |
| LicenseDocument | customer_id, reviewed_by, số/hạng/hạn GPLX, ảnh, trạng thái | Giữ lịch sử nộp/duyệt |
| Payment | booking_id, payment_type, amount, status, paid_at | DEPOSIT/FINAL/REFUND, một record/type trong bản đơn giản |
| Promotion | code, discount_type, discount_value, start_date, end_date, quantity | Percent/fixed |
| Review | booking_id unique, rating, comment, created_at | Một đánh giá/booking |
| VehicleInspection | vehicle_id, scheduled_at, completed_at, status, notes | Giữ kiểm định đơn giản |
| Issue | booking_id, slot_id, vehicle_id, employee_id, issue_type, subject, fine_amount, evidence_url, status | Nối đủ bốn thực thể như conceptual |

## Các quyết định cần nhóm duyệt

1. **Biển số–xe:** base đề xuất một biển số chỉ gắn tối đa một xe hiện hành; xe được chưa có biển số. Khác đường nối 1:N hiện tại. Nếu cần lịch sử chuyển biển số, cần mô hình khác, không bỏ unique tùy tiện.
2. **Booking–xe:** base cho `vehicle_id` null lúc đặt theo loại/slot, sau đó staff gán xe. Khác dấu bắt buộc trên conceptual, nhưng phù hợp luồng UI. Trước chuyển `active`, bắt buộc xe/biển số hợp lệ.
3. **Slot:** người dùng xác nhận ý nghĩa khung giờ. `capacity`, open/closed, đặt toàn slot là đề xuất triển khai đơn giản. Booking lưu giờ snapshot. Có nhiều xe cùng loại nên một slot có nhiều booking; không hiểu slot là chỗ đỗ.
4. **Slot trùng nhau:** MVP đề xuất staff tạo các slot không chồng lấn cho cùng loại xe. SQL unique chỉ chặn slot trùng hoàn toàn, chưa chặn giao nhau. Kiệt phải làm kiểm tra nghiệp vụ và khóa transaction khi đặt; schema/base chưa giải quyết concurrent booking.
5. **Sự cố:** hiện conceptual yêu cầu booking/slot/xe/nhân viên. Base giữ FK bắt buộc. Nếu muốn ghi nhận sự cố trước khi gán nhân viên hoặc ngoài booking, cần đổi nullability có thống nhất.
6. **Tiền cọc:** UI có cọc lớn hơn tiền thuê; không tự coi cọc là tiền trả trước trừ vào tiền thuê. Nhóm cần chốt cọc bảo đảm, hoàn cọc khi trả xe, hủy >72h và phí phạt. Base chỉ chuẩn bị trường/record thanh toán; chưa chạy nghiệp vụ tiền.
7. **GPLX:** physical có một ảnh; UI có ảnh trước/sau. Base giữ một ảnh để đơn giản; mở rộng thêm ảnh khi nhóm chốt phạm vi upload.
8. **Giao/nhận tận nơi:** giữ trường địa chỉ trong entity để không mất thiết kế cũ; giai đoạn đầu có thể dùng nhận/trả tại cửa hàng. Chưa làm bản đồ/tính phí khoảng cách.

## Điều kiện nghiệp vụ cần triển khai

- Loại xe của vehicle được gán phải trùng loại xe của slot.
- `delivery_time/scheduled_end_time` của booking phải phù hợp slot đã chọn.
- Kiểm tra capacity và các booking còn hiệu lực, gồm trạng thái chờ cọc đã giữ chỗ.
- Xe maintenance/rented hoặc đang có booking chồng thời gian không được gán.
- Không tin giá/discount/customer_id/role gửi từ form; service lấy và tính từ database/session.
- Issue phải dùng đúng slot/vehicle của booking; form không được tự chọn tổ hợp sai.
- Review: khách sở hữu booking, đã completed, chưa review; rating 1..5.
- Thanh toán/hủy đổi trạng thái trong transaction, ngăn ghi tiền hai lần.

Các điều kiện liên bảng trên chưa được đảm bảo chỉ bởi FK và CHECK trong SQL. Chủ module phải hoàn thành trước khi đánh dấu chức năng xong.
