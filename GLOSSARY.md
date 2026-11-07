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
Phần tiền thuê khách trả trước khi đặt xe, theo tỷ lệ trong điều khoản hợp đồng (hiện là 50%). Chỉ là trả trước tiền thuê, không phải khoản bảo đảm để trừ phí. Chỉ được hoàn khi khách huỷ trước hạn huỷ miễn phí, trong Thời gian huỷ sau cọc, hoặc khi bên cho thuê không còn xe cùng loại để giao.
_Avoid_: Tiền thế chân, tiền bảo đảm

**Phí trả xe (Return charge)**:
Khoản phí nhân viên ghi nhận lúc nhận lại xe — vệ sinh, phí cầu đường, khác — không có biên bản, không gửi thông báo riêng, cộng thẳng vào Quyết toán. Hư hỏng không phải Phí trả xe mà phải lập Sự cố. Không thu tiền bù nhiên liệu: mức nhiên liệu chỉ được ghi trong Bàn giao.
_Avoid_: Phụ phí, surcharge

**Ân hạn trả xe (Late return grace)**:
Khách trả xe trễ dưới 1 giờ so với giờ trả dự kiến thì không bị tính phí trễ. Phí trễ chỉ tính theo số giờ trễ tròn: trễ 1 giờ 59 phút tính 1 giờ, trễ 2 giờ tính 2 giờ.
_Avoid_: Làm tròn lên, phạt theo phút

**Thời gian huỷ sau cọc (Post-deposit cancellation window)**:
Khoảng thời gian ngắn ngay sau khi khách thanh toán cọc (theo điều khoản hợp đồng, hiện là 1 giờ) mà khách huỷ đơn vẫn được hoàn 100% Tiền cọc, kể cả khi đơn đã nằm trong khoảng không được hoàn. Chỉ áp dụng khi chưa tới giờ nhận xe.
_Avoid_: Ân hạn huỷ, thời gian suy nghĩ

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

**Thời gian chuẩn bị xe (Turnaround time)**:
Khoảng cách tối thiểu 1 giờ giữa giờ kết thúc của lượt thuê trước và giờ bắt đầu của lượt thuê sau trên cùng một xe, dành cho kho nhận lại và chuẩn bị xe. Việc chuẩn bị xe diễn ra ngoài hệ thống. Xe không được giao sớm hơn giờ hẹn.
_Avoid_: Đệm, buffer, giờ nghỉ, khoảng trống, downtime, nhận xe sớm

**Đổi xe (Vehicle swap)**:
Thay xe đã gán cho đơn thuê bằng một xe cùng loại còn trống trong khung giờ thuê, khi xe đang gán chưa về hoặc chưa sẵn sàng. Không bao giờ đổi sang loại xe khác. Chỉ làm được trước khi Bàn giao; xe khách thực sự nhận là xe ghi trong biên bản Bàn giao, dù hợp đồng đã phát hành ghi xe dự kiến khác. Nếu không còn xe cùng loại, đơn bị huỷ, khách được hoàn 100% Tiền cọc tại quầy dù đã quá hạn huỷ miễn phí và nhận Voucher bù.
_Avoid_: Nâng hạng, phân khúc cao hơn, xe tương đương

**Voucher bù (Compensation voucher)**:
Mã giảm 10% tiền thuê, dùng 1 lần trong 90 ngày, tặng riêng cho khách có đơn bị huỷ vì bên cho thuê không còn xe cùng loại để giao. Chỉ là giảm giá cho lần thuê sau, không bao giờ quy đổi thành tiền mặt.
_Avoid_: Hoàn tiền thêm, bồi thường bằng tiền

**Bàn giao (Handover)**:
Biên bản ghi nhận lúc giao xe cho khách (nhận xe) hoặc nhận lại xe từ khách (trả xe): số km, mức nhiên liệu, ảnh giấy tờ, ghi chú. Không phải Sự cố và không mang phí.
_Avoid_: Sự cố bàn giao, issue check-in

**Khách không đến nhận xe (No-show)**:
Đơn đã đặt cọc mà khách không nhận xe trong Thời gian chờ nhận xe. Đơn tự động bị huỷ, Tiền cọc không được hoàn, và xe được trả về lịch trống.
_Avoid_: Bỏ đơn, huỷ trễ

**Thời gian chờ nhận xe (Pickup wait)**:
Khoảng thời gian sau giờ nhận dự kiến mà kho vẫn giữ xe cho khách (theo điều khoản hợp đồng, hiện là 2 giờ), không vượt quá giờ trả dự kiến. Khách đến trong khoảng này vẫn nhận được xe nhưng giờ trả không đổi.
_Avoid_: Giữ chỗ, nhận xe trễ

**Thời điểm nhận xe thực tế (Actual Pickup Time)**:
Thời điểm nhân viên hoàn tất thủ tục bàn giao xe (Check-in) trên hệ thống, chuyển trạng thái đơn thuê sang `active`. Mọi sự cố và trách nhiệm bồi thường của khách được kích hoạt có hiệu lực kể từ thời điểm này.
_Avoid_: Giờ hẹn, scheduled pickup
