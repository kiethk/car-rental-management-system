# Quy ước code (backend)

Ngắn gọn, chỉ những điều đã thống nhất khi dọn code (11/2026). Thuật ngữ nghiệp vụ nằm ở `GLOSSARY.md`.

## Phân tầng

| Tầng | Package | Làm gì | Không làm gì |
|------|---------|--------|--------------|
| Controller | `controller` | Đọc request, gọi service, đổ dữ liệu vào model, chọn view/redirect | Không phân tích ngày giờ thủ công, không lọc/tính nghiệp vụ, không dùng repository |
| Service | `service` (interface) + `service/impl` | Nghiệp vụ, giao dịch | Không biết về HTTP/session |
| Repository | `repository` | Truy vấn | Không chứa nghiệp vụ |
| DTO | `dto` | Dữ liệu ra/vào trang. DTO chỉ đọc dùng `record`; DTO nhận form thì là class có getter/setter | Không chứa logic nghiệp vụ |
| Sự kiện | `event` | Chỉ các record `*Event` | Không chứa listener |
| Thông báo | `notification` | `CustomerMailer` và mọi thứ gửi email cho khách | Không tự dựng `MimeMessage` ngoài `CustomerMailer` |

Một service chỉ có một vai trò. Ví dụ đơn đặt xe tách làm ba:

- `BookingService`: vòng đời (tạo, huỷ, đổi xe);
- `BookingQueryService`: chỉ đọc (danh sách, chi tiết, đếm);
- `VehicleAvailabilityService`: xe nào trống khi nào.

Thanh toán tách làm hai: `VnPayGateway` (ký URL và kiểm chữ ký VNPay) và `PaymentService` (sổ thanh toán, hoàn tiền).

## Đặt tên

- `*Listener`: phản ứng với một sự kiện (`@TransactionalEventListener`).
- `*Notifier`: tác vụ định kỳ (`@Scheduled`) có gửi email.
- Hàm cùng tên khác nghĩa thì đổi tên theo nghĩa (`cancelUnpaidBooking` / `cancelBooking`), không dùng overload.

## Hằng số và cấu hình

- Trạng thái đơn đặt xe: dùng `Booking.STATUS_*`; trạng thái xe: `Vehicle.STATUS_*`; trạng thái thanh toán: `Payment.STATUS_*`. Không gõ lại chuỗi (`StatusConstantsTest` sẽ báo lỗi).
- Múi giờ Việt Nam: chỉ `DateTimeUtils.VIETNAM_ZONE` / `VIETNAM_OFFSET`.
- Tên, địa chỉ, điện thoại, bản đồ của kho: chỉ ở `crms.station.*` (đọc qua `StationProperties`). Trang, email và PDF hợp đồng đều lấy từ đây.
- Ảnh dòng xe (có ảnh mặc định): `VehicleImages.urlOf(...)`.

## Email

Mọi email đi qua `CustomerMailer.send(...)`. Muốn thêm email mới: thêm template trong `templates/mail/`, thêm một listener/notifier gọi `CustomerMailer`, và test bằng cách mock `CustomerMailer`.

## Ngày giờ trên trang

Cửa sổ thuê xe đọc bằng `RentalWindowParser` (chấp nhận `startDate/endDate/date/startTime/endTime`, thiếu hoặc sai thì dùng mặc định). Kiểm tra hợp lệ do `BookingService.validateRentalSchedule` làm, không phải controller.
