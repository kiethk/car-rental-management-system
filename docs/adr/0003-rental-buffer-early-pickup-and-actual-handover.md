# ADR 0003: Quy định khoảng đệm chuyển ca (Buffer Time), Nhận xe sớm (Early Pickup) và Ghi nhận sự cố theo thời gian thực tế

- **Trạng thái**: Đã chấp nhận (Accepted)
- **Ngày**: 2026-11-02
- **Tác giả**: Team CRMS & Pair Programming Partner

## Bối cảnh

Trong hệ thống cho thuê xe trước đây:
1. Hai booking trên cùng 1 xe có thể xếp liền kề nhau với khoảng đệm 0 phút (`Booking A` kết thúc lúc 12:00 thì `Booking B` có thể bắt đầu lúc 12:00). Khi đó, nếu khách trước trả trễ dù chỉ 5 phút, ca sau sẽ bị chậm trễ bàn giao và xe chưa được vệ sinh, kiểm tra.
2. Việc tạo Sự cố (`Issue`) kiểm tra thời điểm xảy ra sự cố với `booking.pickupTime` (giờ dự kiến). Nếu khách nhận xe sớm (ví dụ nhận lúc 10:30 thay vì 11:00) và gặp sự cố ngay lúc 10:45, hệ thống từ chối tạo sự cố vì "thời gian xảy ra trước thời gian nhận xe", dù đơn thuê đã `active` và khách đã nhận xe thực tế.
3. Chưa có cơ chế chuẩn hóa thời gian nhận xe sớm và thông báo khi xe ca trước chưa kịp bàn giao.

## Quyết định

1. **Khoảng đệm chuyển ca (Buffer Time = 1 giờ)**:
   - Mọi lịch đặt xe trên cùng một phương tiện vật lý bắt buộc phải cách nhau tối thiểu 1 giờ.
   - Khoảng đệm 1 giờ được phân bổ:
     - 30 phút đầu: Vệ sinh xe và kiểm tra kỹ thuật sau ca thuê trước.
     - 30 phút sau: Xe sẵn sàng bàn giao đúng giờ hoặc phục vụ khách nhận sớm.
   - Khi tìm kiếm xe khả dụng hoặc phân bổ xe (`assignVehicle`), hệ thống tự động cộng đệm $\pm 1$ giờ để tránh xung đột lịch.

2. **Quy định Nhận xe sớm (Early Pickup Window = 30 phút)**:
   - Khách chỉ được phép làm thủ tục nhận xe (Check-in) trước giờ hẹn tối đa 30 phút (`now >= scheduled_pickup_time - 30m`).
   - Nếu đến sớm hơn 30 phút, hệ thống từ chối check-in với thông báo rõ ràng về giờ nhận dự kiến.
   - **Điều kiện xe sẵn sàng (`vehicle.status == available`)**:
     - Nếu khách đến sớm trong khung 30 phút nhưng xe vẫn chưa sẵn sàng (đang dọn dẹp hoặc ca trước trả trễ): Hệ thống từ chối check-in sớm kèm hướng dẫn khách có thể đợi đến đúng giờ hẹn dự kiến, hoặc liên hệ nhân viên đổi sang xe khác cùng loại (khác biển số) để nhận ngay.
     - Nếu ca trước trả trễ dưới 30 phút, khi khách ca sau đến đúng giờ hẹn dự kiến, xe đã vệ sinh xong và được bàn giao bình thường.

3. **Thời điểm nhận xe thực tế (`actual_pickup_time`) và Trách nhiệm Sự cố**:
   - Thêm cột `actual_pickup_time` vào bảng `bookings` và entity `Booking`.
   - Khi check-in thành công: gán `actual_pickup_time = NOW()`, đổi trạng thái booking thành `active`, trạng thái xe thành `rented`.
   - Kiểm tra thời gian xảy ra Sự cố (`occurredAt`) dựa trên `actual_pickup_time` thực tế thay vì giờ hẹn dự kiến. Khách có thể lập biên bản sự cố ngay khi xe đã được bàn giao thực tế.
   - Khi tất toán (`settle`), mọi sự cố `OPEN` gắn với đơn thuê được tự động chuyển thành `RESOLVED`.

## Hệ quả & Đánh giá

- **Tích cực**:
  - Đảm bảo chất lượng xe (luôn có tối thiểu 30 phút dọn dẹp vệ sinh trước khi giao ca tiếp theo).
  - Tránh tình trạng khách nhận xe sớm bị va quẹt mà không thể lập biên bản sự cố.
  - Xử lý mượt mà kịch bản ca trước trả trễ nhẹ mà không gây vỡ lịch ca sau.
  - Trao quyền chủ động cho nhân viên tại quầy đổi xe cùng dòng khi khách muốn nhận sớm lúc xe cũ chưa kịp dọn.
