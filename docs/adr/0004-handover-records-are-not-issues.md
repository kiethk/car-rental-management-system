# ADR 0004: Biên bản bàn giao tách khỏi bảng Sự cố

- **Trạng thái**: Đã chấp nhận
- **Ngày**: 2026-11-02

## Bối cảnh

Biên bản nhận xe (CHECK_IN) và trả xe (CHECK_OUT) từng được lưu như một dòng trong bảng `issues`, với số km và mức nhiên liệu ghép vào chuỗi `subject`. Vì vậy:

- chúng lẫn vào danh sách Sự cố, thống kê "đang mở" và trang của khách;
- nhân viên có thể sửa hoặc huỷ chúng như một Sự cố. Huỷ biên bản "Trả xe" còn xoá luôn phí trễ, vì phí trễ được gắn vào biên bản đó;
- chúng mâu thuẫn với định nghĩa trong GLOSSARY: một Sự cố luôn có Phí sự cố > 0.

## Quyết định

- Biên bản bàn giao được lưu ở bảng riêng `handovers`. Mỗi đơn có tối đa một biên bản cho mỗi loại, với các cột thật cho số km, % nhiên liệu, ghi chú, ảnh giấy tờ, và `settled_at` cho biên bản trả xe.
- Phí trễ và Phí trả xe gắn vào đơn thuê (`booking_fees.issue_id = NULL`). Chỉ Phí sự cố mới gắn vào một Sự cố.
- Bảng `issues` chỉ còn các loại COLLISION, DAMAGE, TRAFFIC_FINE, OTHER.

## Phương án đã cân nhắc

- **Giữ chung bảng và lọc CHECK_IN/CHECK_OUT ở mọi nơi**: không cần đổi schema. Bị loại vì mỗi truy vấn mới đều phải nhớ lọc, quên một lần là lỗi tái diễn.

## Hệ quả

Mọi máy trong team phải chạy lại `database/schema.sql` rồi `seed.sql`.
