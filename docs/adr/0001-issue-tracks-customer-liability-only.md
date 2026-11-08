# Sự cố chỉ theo dõi trách nhiệm tiền của khách, không theo dõi việc sửa xe

Một Sự cố kéo theo hai luồng công việc với nhịp thời gian khác nhau: thống nhất và thu Phí sự cố (vài phút đến vài ngày, có khi sau Quyết toán) và sửa xe (có thể nhiều tuần, hoặc không liên quan, như Phạt nguội). (Cập nhật 2026-11-08, ADR 0005: Phạt nguội không còn là Sự cố mà là Bản ghi phạt nguội riêng, không có tiền.) Chúng tôi quyết định Sự cố chỉ mô hình hoá luồng thứ nhất: vòng đời `open → resolved / cancelled` nói về Phí sự cố, luôn gắn với một đơn thuê khách đã nhận xe, và số tiền chỉ nằm trong `booking_fees` (không có `fine_amount` trên `issues`). Việc sửa xe do trạng thái xe (`maintenance`, `retired`) và kiểm định xe đảm nhiệm.

## Considered Options

- **Một vòng đời chung cho cả tiền và sửa xe** (`open → in_progress → resolved` như issue gốc): bị loại vì "resolved" sẽ phải chờ xe sửa xong mới thu được tiền, và Phạt nguội không có gì để sửa.
- **Cho phép Sự cố không gắn đơn thuê** (xe hỏng ở bãi): bị loại vì không có khách để quy trách nhiệm; đổi trạng thái xe là đủ.
- **Lưu `fine_amount` trên `issues` rồi sao sang `booking_fees` khi quyết toán**: bị loại vì tạo hai nguồn sự thật cho cùng một số tiền.

## Consequences

- "Khoá" thật sự là khi Phí sự cố đã được thanh toán (`booking_fees.payment_id` trỏ tới payment `paid`), không phải khi `resolved`; admin vẫn mở lại/huỷ được Sự cố đã chốt mà chưa thanh toán.
- Không có báo cáo "xe đang sửa vì sự cố nào" từ bảng `issues`; nếu cần sẽ phải liên kết từ phía bảo dưỡng/kiểm định xe.
