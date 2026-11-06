# Vòng đời phí sự cố tự động: thông báo tức thì, thanh toán linh hoạt và đồng bộ quyết toán

Thay vì quy trình chốt thủ công (`resolve`) rồi mới thông báo đòi nợ sau quyết toán, chúng tôi thống nhất chuyển sang mô hình tự động hóa toàn diện vòng đời Sự cố và Phí sự cố:

1. **Thông báo tức thì khi tạo sự cố**: Một Sự cố chỉ được ghi nhận khi đã có đầy đủ thông tin và xác định được mức phí cụ thể (> 0). Ngay khi lập sự cố (dù xe đang trong chuyến đi hay đã trả xe xong), hệ thống tự động gửi email thông báo kèm link thanh toán trực tuyến ngay lập tức cho khách hàng.
2. **Khách hàng thanh toán linh hoạt**: Khách hàng có thể thanh toán trực tuyến qua cổng VNPay ngay khi đang trong chuyến đi (ví dụ: đang ở khách sạn) hoặc sau chuyến đi. Khi giao dịch thanh toán thành công, Sự cố tự động chuyển sang trạng thái `RESOLVED` (khoản phí chuyển sang `PAID`) và bị khóa bất biến.
3. **Thanh toán trực tiếp tại quầy**: Đối với các trường hợp khách hàng nộp tiền mặt hoặc chuyển khoản trực tiếp vào tài khoản công ty, nhân viên có quyền thực hiện thao tác "Ghi nhận thu tiền tại quầy" trên sự cố đang `OPEN` để tạo giao dịch thanh toán và tự động chuyển sự cố sang `RESOLVED`.
4. **Đồng bộ Quyết toán trả xe (`checkOut`)**:
   - Khi trả xe, hệ thống chỉ gom các sự cố đang `OPEN` (chưa thanh toán) vào số tiền khách phải trả khi quyết toán. (Sửa 2026-11-03: tiền cọc là phần tiền thuê trả trước, tối đa 100% tiền thuê, nên không bao giờ dư để trừ phí.)
   - Các sự cố khách đã thanh toán trước qua VNPay tuyệt đối không bị tính lại vào công nợ quyết toán, và được hiển thị minh bạch trong mục "Đã thanh toán trước" trên hóa đơn quyết toán.
   - Khi Quyết toán hoàn tất, toàn bộ các sự cố `OPEN` được thu cùng quyết toán sẽ tự động chuyển trạng thái sang `RESOLVED`.
5. **Chống xung đột thanh toán đa kênh (Omnichannel concurrency)**:
   - Một sự cố đang có phiên thanh toán VNPay `pending` (thời hạn 15 phút) sẽ chặn/cảnh báo thao tác Quyết toán trả xe của nhân viên cho đến khi phiên kết thúc hoặc bị hủy, ngăn chặn tuyệt đối tình huống khách bị trừ tiền hai lần (vừa nộp online vừa trả tại quầy khi quyết toán).
6. **Điều chỉnh phí và Hủy sự cố khi đang `OPEN`**:
   - Nhân viên chỉ có thể chỉnh sửa mức phí hoặc hủy bỏ khi sự cố còn đang `OPEN`.
   - Khi sửa mức phí (sau khi thương lượng với khách), hệ thống gửi email cập nhật mức phí mới và hủy phiên thanh toán cũ để tránh thanh toán sai số tiền.
   - Khi hủy sự cố, hệ thống xóa khoản phí phát sinh và gửi email xác nhận hủy bỏ nghĩa vụ bồi thường cho khách.
7. **Khóa bất biến sau khi `RESOLVED`**:
   - Khi đã `RESOLVED` (tiền đã thu qua VNPay, tại quầy, hoặc cùng quyết toán trả xe), Sự cố bị khóa vĩnh viễn, không được sửa, không được hủy và không có cơ chế `reopen`.

## Considered Options

- **Giữ cơ chế Resolve thủ công trước khi thông báo**: Bị loại vì nhân viên có thể bấm chốt khống khi tiền chưa thu, và khách không biết trước chi phí để chủ động thanh toán trong chuyến đi.
- **Cho phép tạo sự cố tạm thời với phí = 0đ**: Bị loại vì tạo biên bản rác và gây khó hiểu cho khách khi nhận thông báo nợ chưa rõ số tiền.
- **Cho phép Reopen/Cancel sự cố đã RESOLVED**: Bị loại vì làm sai lệch sổ sách kế toán và hóa đơn hợp đồng đã thanh toán.
