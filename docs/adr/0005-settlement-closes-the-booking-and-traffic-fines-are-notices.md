# ADR 0005: Tiền thuê thu lúc bàn giao, quyết toán đóng đơn, phạt nguội chỉ là thông báo

- **Trạng thái**: Đã chấp nhận
- **Ngày**: 2026-11-08

## Bối cảnh

Quyết toán từng thu cả phần tiền thuê còn lại, và còn cho phép lập Sự cố (kể cả phạt nguội) sau khi đơn đã hoàn tất để khách thanh toán trực tuyến riêng. Điều đó kéo theo nhiều ràng buộc rườm rà: đơn đã "hoàn tất" vẫn có thể có tiền chưa thu, khách thuê tiếp trong khi còn nợ, và phạt nguội bị hiểu như một khoản mình phải đòi. Thực tế phạt nguội do công an xử lý trực tiếp với khách, hệ thống chỉ cần báo cho khách biết.

## Quyết định

1. **Tiền thuê còn lại thu lúc Bàn giao** (`Payment.TYPE_RENTAL_BALANCE`, tiền mặt hoặc chuyển khoản do nhân viên ghi nhận). Chưa thu thì chưa giao xe. **Quyết toán chỉ thu các khoản phí chưa thanh toán** (phí trễ, Phí trả xe, Phí sự cố) và không có số âm để kiểm tra.
2. **Quyết toán đóng đơn**: Sự cố chỉ ghi nhận cho đơn đang thuê (`active`), phải đủ trước khi quyết toán. Bỏ khái niệm "Phí phát sinh sau quyết toán" và việc khách thanh toán phí sự cố cho đơn đã hoàn tất.
3. **Phạt nguội tách khỏi Sự cố**: không còn `IssueType.TRAFFIC_FINE`. Bản ghi riêng `traffic_fine_notices` (biển số, thời điểm, ảnh biên bản trong thư mục riêng `uploads/traffic-fines/`, nhân viên lập) không có số tiền và không có thanh toán. Tab "Phạt nguội" ở trang admin có tra cứu đơn theo biển số và thời điểm, tạo bản ghi, xem danh sách và xóa (nhân viên hoặc admin, vì chỉ là thông báo). Tạo hoặc xóa đều gửi email cho khách; khách thấy bản ghi trên đơn của mình. Bản ghi tạo được cả sau quyết toán.
4. **Chốt chặn đơn chưa quyết toán**: khách đã trả xe mà chưa quyết toán thì không đặt đơn mới; khách còn đơn `active` thì đơn sau không được bàn giao. Đơn sau vẫn giữ xe trong Thời gian chờ nhận xe; hết thời gian đó mà chưa bàn giao được thì cơ chế khách không đến nhận xe tự huỷ đơn và không hoàn cọc, không cần bộ hẹn giờ riêng.
5. **Bỏ nhiên liệu lúc trả xe và Phí trả xe**: biên bản trả xe chỉ ghi số km (mức nhiên liệu chỉ ghi lúc giao xe, `handovers.fuel_percent` để trống ở biên bản trả). Không còn form Phí trả xe (vệ sinh, cầu đường, khác); khoản nào phát sinh thật sự thì lập Sự cố loại "Khác" kèm mức phí. Phí trễ vẫn tự tính.
6. **Kho hoạt động 24/24**: giờ nhận và giờ trả là bất kỳ giờ chẵn nào. Không còn phải chọn thuê tới 6 giờ sáng hôm sau để qua đêm.

## Phương án đã cân nhắc

- **Giữ phạt nguội là một loại Sự cố với phí = 0**: bị loại vì mâu thuẫn với định nghĩa Sự cố luôn có Phí sự cố > 0 (ADR 0001, 0002), và sinh ra thanh toán không có thật.
- **Thu 50% còn lại qua VNPay trước khi nhận xe**: bị loại vì thêm phiên thanh toán 15 phút và xử lý khách không trả; nhân viên đã có sẵn cách ghi nhận tiền mặt hoặc chuyển khoản lúc quyết toán.

## Hệ quả

- Mọi máy trong team phải chạy lại `database/schema.sql` rồi `seed.sql` (bảng mới `traffic_fine_notices`, ràng buộc loại Sự cố không còn `TRAFFIC_FINE`).
- ADR 0002 mục 1 và 4 vẫn đúng với đơn đang thuê; phần "đã trả xe xong" nay nghĩa là đã ghi nhận trả xe nhưng chưa quyết toán.
- Sự cố đã lập mà khách chưa trả vẫn được thu cùng quyết toán (ADR 0002), vì quyết toán chốt toàn bộ phí còn mở.
