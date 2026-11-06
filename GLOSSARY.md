# CRMS — Car Rental Management System

Hệ thống cho thuê xe tự lái: khách đặt đơn thuê theo loại xe, nhân viên giao/nhận xe, quyết toán và xử lý các phát sinh trong chuyến thuê.

## Language

### Quyết toán & phí

**Quyết toán (Settlement)**:
Lần chốt tiền khi khách trả xe: khách trả phần tiền thuê còn lại (tiền thuê trừ Tiền cọc đã đặt) cộng phí trễ, Phí trả xe và các Phí sự cố chưa thanh toán. Phí không bao giờ được trừ vào cọc, và Quyết toán không bao giờ hoàn tiền. Sự cố khách đã thanh toán trước đó không bị thu lại.
_Avoid_: Thanh toán cuối, checkout payment

**Phí sự cố (Issue compensation)**:
Khoản tiền khách phải trả gắn với một Sự cố — bắt buộc phải có mức phí cụ thể (> 0) khi lập Sự cố; mỗi Sự cố có duy nhất một khoản phí.
_Avoid_: Tiền phạt (chung chung), fine amount, phụ phí, additional fine

**Tiền cọc (Deposit)**:
Phần tiền thuê khách trả trước khi đặt xe, theo tỷ lệ trong điều khoản hợp đồng (hiện là 50%). Chỉ là trả trước tiền thuê, không phải khoản bảo đảm để trừ phí; chỉ được hoàn khi khách huỷ đơn đúng hạn.
_Avoid_: Tiền thế chân, tiền bảo đảm

**Phí trả xe (Return charge)**:
Khoản phí nhân viên ghi nhận lúc nhận lại xe — vệ sinh, thiếu nhiên liệu, phí cầu đường, khác — không có biên bản, không gửi thông báo riêng, cộng thẳng vào Quyết toán. Hư hỏng không phải Phí trả xe mà phải lập Sự cố.
_Avoid_: Phụ phí, surcharge

**Phí phát sinh sau quyết toán**:
Phí sự cố (như phạt nguội) được phát hiện và lập biên bản sau khi đơn thuê đã hoàn tất trả xe, khách nhận email và thanh toán trực tuyến riêng.
_Avoid_: Nợ, công nợ, phí bổ sung

**Phiên thanh toán sự cố (Issue payment session)**:
Lượt thanh toán trực tuyến của khách cho một hoặc nhiều Phí sự cố, có hiệu lực trong 15 phút.
_Avoid_: Đơn hàng mới, checkout lần hai

### Sự cố

**Sự cố (Issue)**:
Một biến cố trong thời gian thuê mà khách phải chịu trách nhiệm bồi thường — va chạm, hư hỏng, phạt nguội; luôn gắn với một đơn thuê và luôn kèm theo mức phí cụ thể khi được ghi nhận.
_Avoid_: Incident, biên bản, ticket

**Chốt (Resolve)**:
Trạng thái hoàn tất nghĩa vụ tài chính của Sự cố — đạt được ngay khi khách thanh toán trực tuyến thành công, hoặc khi khoản phí được thu cùng Quyết toán trả xe.
_Avoid_: Đóng, giải quyết xong (khi chưa trả tiền)

**Phạt nguội (Traffic fine)**:
Biên bản phạt vi phạm giao thông do cơ quan chức năng gửi về chủ xe sau khi vi phạm, xác định theo biển số và thời điểm vi phạm.
_Avoid_: Vi phạm giao thông (khi phạt tại chỗ — khách tự nộp, không phải Sự cố)

**Người lập biên bản (Reporter)**:
Nhân viên ghi nhận Sự cố vào hệ thống; không đổi về sau.
_Avoid_: Người xử lý, assignee

**Thời gian chờ thông báo (Notice grace period)**:
30 giây đầu sau khi lập Sự cố, khách chưa được thông báo. Huỷ trong khoảng này coi như Sự cố chưa từng tồn tại với khách; sửa phí trong khoảng này chỉ dẫn tới một thông báo duy nhất với mức phí cuối.
_Avoid_: Nháp, draft

### Xe

**Ngừng khai thác (Retired)**:
Trạng thái của xe không còn được cho thuê vĩnh viễn, ví dụ do tổn thất toàn bộ hoặc không thu hồi được.
_Avoid_: Thanh lý, xoá xe, inactive

**Tổn thất toàn bộ (Total loss)**:
Xe hư hỏng không thể sửa chữa, hoặc chi phí sửa chữa vượt ngưỡng hợp đồng quy định so với giá trị xe.
_Avoid_: Hư nặng, hỏng hẳn

### Lịch trình & Bàn giao xe

**Thời gian đệm chuyển ca (Rental Buffer Time)**:
Khoảng cách tối thiểu bắt buộc là 1 giờ giữa hai lượt thuê liên tiếp trên cùng một phương tiện vật lý. Được phân bổ thành:
1. 30 phút đầu: Vệ sinh xe, kiểm tra kỹ thuật sau khi ca thuê trước trả xe.
2. 30 phút sau: Dự phòng và sẵn sàng bàn giao cho ca thuê tiếp theo (đón khách đúng giờ hoặc nhận sớm).
_Avoid_: Giờ nghỉ, khoảng trống, downtime

**Khung giờ nhận sớm (Early Pickup Window)**:
Khoảng thời gian tối đa 30 phút trước giờ nhận dự kiến (`scheduled_pickup_time - 30 phút`) mà khách có thể làm thủ tục nhận xe, với điều kiện tiên quyết là phương tiện đã ở trạng thái sẵn sàng (`available`).
_Avoid_: Lấy xe tuỳ tiện, nhận xe bất cứ lúc nào

**Bàn giao (Handover)**:
Biên bản ghi nhận lúc giao xe cho khách (nhận xe) hoặc nhận lại xe từ khách (trả xe): số km, mức nhiên liệu, ảnh giấy tờ, ghi chú. Không phải Sự cố và không mang phí.
_Avoid_: Sự cố bàn giao, issue check-in

**Thời điểm nhận xe thực tế (Actual Pickup Time)**:
Thời điểm nhân viên hoàn tất thủ tục bàn giao xe (Check-in) trên hệ thống, chuyển trạng thái đơn thuê sang `active`. Mọi sự cố và trách nhiệm bồi thường của khách được kích hoạt có hiệu lực kể từ thời điểm này.
_Avoid_: Giờ hẹn, scheduled pickup
