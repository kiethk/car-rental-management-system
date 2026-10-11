# Phân công bàn giao nhóm

Mục tiêu: ứng dụng Spring MVC + Thymeleaf mức project PE, đủ luồng thuê xe, theo conceptual 13 thực thể. Mỗi người chịu trách nhiệm Java + template + kiểm tra module mình. Deadline chưa được cung cấp; kế hoạch theo mốc tương đối.

## Phân công

| Thành viên | Module chính | File chính | Bàn giao / nghiệm thu |
|---|---|---|---|
| Kiệt | Base, Slot, Booking, Payment, tích hợp | Slot/Booking/Payment service, controller, DTO; templates/bookings, admin booking/payment | Chọn loại+slot → đặt → cọc giả lập → xác nhận → gán xe → nhận → trả → tiền cuối/hủy; kiểm tra capacity/trùng xe/trạng thái/transaction |
| Phú | Customer, Employee, auth, LicenseDocument | auth/profile/license controller/service/DTO; templates/auth, profile, licenses | Đăng ký/đăng nhập/logout; hash password; customer/staff/admin; bảo vệ trang; khách chỉ xem booking của mình; nộp và duyệt GPLX |
| Phúc | VehicleType, Vehicle, LicensePlate, VehicleInspection | fleet controller/service/DTO; templates/vehicles và admin fleet | CRUD loại xe/xe; gán biển số; tìm kiếm; maintenance; lịch và kết quả kiểm định; dữ liệu fleet đủ cho booking |
| Trí | Promotion, Review, Issue | promotion/review/issue controller/service/DTO; templates/promotions, reviews và admin issue | Làm từng chức năng nhỏ theo mẫu: danh sách → form → validate → lưu → trạng thái; không tự tính cọc/phạt hoặc đổi booking |

Trí làm đủ phần code, không chỉ báo cáo. Phúc review khuyến mãi/CRUD; Phú review quyền/đánh giá; Kiệt review liên kết Issue với booking. Nếu thiếu thời gian, giảm giao diện/trang trí hoặc tính năng nâng cao; vẫn giữ module và luồng chính đã thống nhất.

Kiệt giữ entity/schema chung. Sau mốc 0, các thành viên chủ yếu tạo DTO/service/controller/template của module; thay entity qua PR riêng để tránh xung đột. Không để mọi người cùng sửa một AdminController hoặc một template index lớn.

## Mốc 0 — một buổi chốt thiết kế

- Đọc CURRENT-STATE và ERD-REVIEW. Chốt physical 13 bảng; đặc biệt quan hệ biển số–xe, booking–xe, capacity slot, chính sách cọc.
- Chạy base trên máy cả 4 người; mỗi người mở danh sách xe và chạy `mvn verify`.
- Chốt tên repo/visibility, tạo repo khi Kiệt xác nhận; mỗi người clone và tạo nhánh chức năng.
- Chốt đường dẫn và field DTO; chưa đổi schema riêng lẻ.

## Mốc 1 — 3–4 buổi: nền tảng và CRUD

- Kiệt: tạo slot theo loại, danh sách slot mở, form booking + tính giá phía service.
- Phú: auth và role/session, guard customer/staff; trang profile.
- Phúc: VehicleType/Vehicle/LicensePlate CRUD, dữ liệu mẫu ổn định.
- Trí: Promotion list/add/edit, validate code, ngày, percent/fixed, quantity; PR đầu tiên nhỏ để học quy trình.
- Bàn giao: đăng nhập được, có xe/slot, tạo booking `pending_payment` và lưu DB.

## Mốc 2 — 3–4 buổi: luồng thuê hoàn chỉnh

- Kiệt: thanh toán mô phỏng, confirm/gán xe, nhận/trả, hủy, chống booking vượt capacity/trùng xe.
- Phú: GPLX upload/duyệt; kiểm tra GPLX trong service lúc cho thuê. Không chỉ chặn ở giao diện.
- Phúc: maintenance và inspection; phối hợp Kiệt truy vấn xe đủ điều kiện.
- Trí: Review từ booking completed; Issue từ booking đã gán xe, trạng thái open/in_progress/resolved.
- Bàn giao: một lượt customer/staff chạy tới completed; một booking hủy đúng chính sách; không có hai booking gán trùng xe.

## Mốc 3 — 2–3 buổi: tích hợp và demo

- Cùng sửa lỗi, chuyển dần bố cục React sang Thymeleaf cho trang cần demo.
- Trí viết các bước demo, seed minh họa và checklist; mọi người kiểm tra phần mình, review chéo.
- Kiệt merge, kiểm tra từ clone sạch, chạy toàn bộ luồng với PostgreSQL và chuẩn bị bản demo.

Ước lượng 9–12 buổi làm việc; điều chỉnh theo deadline và thời gian thực tế, không phải cam kết ngày hoàn thành.

## Ticket nhỏ giao ngay

| Ticket | Người | Phụ thuộc | Điều kiện xong |
|---|---|---|---|
| BASE-01 | Kiệt | Chốt ERD | 4 máy chạy được; schema thống nhất |
| AUTH-01 | Phú | BASE-01 | Register/login/logout, password hash; dữ liệu tồn tại sau restart |
| FLEET-01 | Phúc | BASE-01 | CRUD loại xe/xe/biển số, validate duplicate, trang danh sách |
| PROMO-01 | Trí | BASE-01, mẫu Vehicle | Danh sách/thêm/sửa mã, ngày hợp lệ, percent ≤100, code unique |
| SLOT-01 | Kiệt | FLEET-01 | Tạo/list khung giờ theo loại, start<end, capacity>0, chặn chồng slot MVP |
| LICENSE-01 | Phú | AUTH-01 | Nộp/duyệt/từ chối GPLX, role và ownership đúng |
| BOOK-01 | Kiệt | AUTH-01,FLEET-01,SLOT-01 | Tạo booking, tính giá/khuyến mãi phía server, capacity an toàn |
| INSPECT-01 | Phúc | FLEET-01 | Lịch/kết quả kiểm định; chặn xe không hợp lệ |
| PAY-01 | Kiệt | BOOK-01, chính sách cọc | Cọc/cuối/hoàn giả lập, idempotent, trạng thái đúng |
| REVIEW-01 | Trí | AUTH-01, BOOK-01 | Chỉ chủ booking completed; tối đa 1 review; rating 1..5 |
| ISSUE-01 | Trí | BOOK-01, fleet, employee | Tạo sự cố đúng booking/slot/xe, giao người xử lý; Kiệt review |
| DEMO-01 | Cả nhóm | Các ticket trên | Clone sạch → setup → demo xuyên suốt không sửa tay DB |

## Hợp đồng giữa module

- Phú cung cấp người dùng hiện tại, role/employee hiện tại và kiểm tra GPLX.
- Phúc cung cấp loại xe/giá, xe+biển số hợp lệ, trạng thái fleet và kiểm định.
- Trí cung cấp promotion hợp lệ (ngày/quantity/giá trị); Kiệt áp dụng trong transaction, không để form tự quyết giá.
- Kiệt cung cấp booking được phép review, booking/slot/xe để tạo Issue, quyền sở hữu booking, vòng đời booking/payment.
- Form dùng DTO và `@Valid`; lỗi trả về form rõ ràng. GET chỉ đọc, POST cho thao tác ghi, có bảo vệ CSRF khi làm auth.

## Phạm vi đơn giản

- Một ứng dụng, một database; form server-rendered, Bootstrap, upload local trong thư mục ngoài source.
- Thanh toán giả lập/manual cho demo; không tích hợp cổng tiền thật trong vòng đầu.
- Slot cố định theo loại xe; nhận/trả cửa hàng trước; dashboard thống kê đơn giản sau khi có dữ liệu thật.
- Chưa cần JWT, microservices, message queue, Redis, real-time hoặc bản đồ.

## Definition of Done

- Code và template chạy cùng PostgreSQL; dữ liệu còn sau restart.
- Có validate và xử lý trường hợp không tồn tại/không đủ quyền.
- Không ghi tiền/đặt vượt chỗ/đổi trạng thái tùy tiện.
- PR mô tả hành vi, cách kiểm tra và thay schema nếu có; người khác review.
- `mvn verify` pass; cập nhật bước setup khi thêm cấu hình.
