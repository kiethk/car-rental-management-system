# ADR 0003: Thời gian chuẩn bị xe 1 giờ giữa hai lượt thuê, không nhận xe sớm, trách nhiệm tính từ lúc nhận xe thực tế

- **Trạng thái**: Đã chấp nhận
- **Ngày**: 2026-11-02, sửa 2026-11-07

## Bối cảnh

Hai lượt thuê trên cùng một xe từng được xếp sát nhau, nên khách trước trả trễ vài phút là ca sau bị trễ theo. Bản đầu của ADR này thêm khung nhận xe sớm 30 phút và một bước hệ thống tự suy luận "xe đã rửa xong chưa" (30 phút sau khi trả). Hai cơ chế đó làm nghiệp vụ khó giải thích mà không thêm giá trị, nên đã được bỏ.

## Quyết định

1. **Thời gian chuẩn bị xe 1 giờ**: hai lượt thuê trên cùng một xe phải cách nhau ít nhất 1 giờ. Vì giờ thuê luôn là giờ chẵn, lượt trước kết thúc 10:00 thì lượt sau sớm nhất bắt đầu 11:00, và ngược lại. Hệ thống áp dụng khi tìm xe trống, khi đặt xe và khi gán xe.
2. **Không nhận xe sớm**: chỉ bàn giao từ đúng giờ hẹn. Lúc bàn giao, xe được gán phải ở trạng thái `available`.
3. **Hậu kỳ ngoài hệ thống**: sau khi trả xe, kho làm thủ tục trả xe trên hệ thống; kiểm tra, rửa xe là việc ngoài đời, hệ thống không theo dõi. Nếu xe chưa về hoặc chưa kịp chuẩn bị đến giờ giao, nhân viên đổi sang xe khác cùng loại cho đơn trước khi bàn giao và tự trao đổi với khách.
4. **Thời điểm nhận xe thực tế** (`actual_pickup_time`): ghi lúc bàn giao xong. Trách nhiệm của khách với Sự cố tính từ thời điểm này, không phải từ giờ hẹn.

## Hệ quả

- Không có trạng thái "đang dọn xe"; nhân viên chịu trách nhiệm đánh giá xe sẵn sàng.
- Thời gian chuẩn bị xe 1 giờ trùng với Ân hạn trả xe: khách trả trễ dưới 1 giờ không bị phạt nhưng có thể khiến ca sau phải đổi xe.
