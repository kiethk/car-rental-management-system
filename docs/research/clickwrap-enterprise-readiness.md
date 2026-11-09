# Clickwrap của CRMS đã chuẩn doanh nghiệp chưa?

Ngày lập: 2026-11-07. Đối chiếu theo working tree của nhánh `feature/phuc-vehicle`, kể cả phần chưa commit.
Đây là nghiên cứu kỹ thuật, không phải tư vấn pháp lý. Nguồn ghi dạng `[S1]`, danh sách ở mục 5. Đường dẫn code tính từ gốc repo, viết tắt: `J/` = `src/main/java/com/crms/mvc/`, `T/` = `src/main/resources/templates/`; `application.properties` nằm ở `src/main/resources/`.

## 1. Kết luận

| Tầng | Kết luận | Lý do chính |
|---|---|---|
| (a) Clickwrap có thể bảo vệ được theo luật Việt Nam | **Đạt về cơ chế giao kết. Chưa đạt về nội dung và công bố** nếu vận hành thật | Giao kết không cần chữ ký số: thông điệp dữ liệu được coi là văn bản [S4 Đ119], hợp đồng điện tử không bị phủ nhận chỉ vì tự động hoá [S1 Đ34]. Nhưng khách chưa thấy toàn văn trước khi đồng ý, hợp đồng thiếu nội dung bắt buộc, không có chính sách bảo mật |
| (b) Bằng chứng cấp doanh nghiệp | **Chưa đạt** | Không có hash, không niêm phong PDF, không dấu thời gian tin cậy, không audit log, bản gốc nằm trên đĩa cục bộ. Mức hiện tại là "bản ghi clickwrap cơ bản" |
| (c) Đồ án môn học | **Đạt, trên mức cần** | Có version bất biến ở tầng service, kiểm tra server-side, IP, PDF, email, khoá chống phát hành trùng. Chỉ cần sửa 2 lỗi nhỏ ở G7 |

Ba điểm dễ hiểu sai:

- Cơ chế này **không phải "ký điện tử" theo nghĩa chặt**. Chỉ chữ ký số hoặc chữ ký điện tử chuyên dùng bảo đảm an toàn mới tương đương chữ ký tay [S1 Đ23(2)]. Clickwrap là sự chấp nhận đề nghị giao kết [S4 Đ400(1)], có thể rơi vào "hình thức xác nhận khác" [S1 Đ22(4)]. Luật không gọi tên clickwrap, nên cách phân loại này còn **không chắc chắn**, nhưng không ảnh hưởng hiệu lực vì hợp đồng thuê xe không có quy định buộc phải ký [S4 Đ117(2), Đ119].
- Hợp đồng có hiệu lực và hợp đồng chứng minh được là hai việc khác nhau. Tầng (b) chỉ làm tăng sức nặng chứng cứ, tức độ tin cậy của cách tạo, lưu, giữ toàn vẹn và xác định người khởi tạo [S1 Đ11(2)].
- DriveNow là doanh nghiệp giả định. Mức "bắt buộc" trong bảng dưới chỉ có nghĩa khi vận hành thật; với đồ án, chúng là tiêu chí nghiệp vụ, không phải vi phạm.

### Đã làm tốt

| Hành vi | Bằng chứng |
|---|---|
| Checkbox không tick sẵn, server kiểm tra lại | `T/bookings/checkout.html:559-566`, `J/dto/BookingCreateDTO.java:25-29`, `J/controller/BookingController.java:346,360`; test `BookingContractFlowTest.java:115-119,123-134` |
| Khách chỉ bị ràng buộc bởi đúng version đã xem; đổi version giữa chừng thì từ chối | `J/service/impl/BookingServiceImpl.java:103-108` |
| Bản ghi chấp nhận và booking cùng một transaction, mỗi booking một contract | `BookingServiceImpl.java:29,196`, `database/schema.sql:179` |
| Lưu thời điểm, IP lấy từ server, version | `J/service/impl/ContractServiceImpl.java:70-84`, `BookingController.java:368-370` |
| Template chỉ sửa được khi DRAFT, tối đa một ACTIVE, FK giữ version đã dùng | `J/service/impl/ContractTemplateServiceImpl.java:117-124`, `schema.sql:180,305-307` |
| Phát hành idempotent bằng khoá bi quan, file không ghi đè | `ContractServiceImpl.java:100-119`, `J/repository/ContractRepository.java:21-23`, `J/service/impl/FileUploadServiceImpl.java:115-116`; test `ContractIssueTest.java:38` |
| PDF riêng tư, kiểm tra chủ sở hữu, khách tải lại được | `J/controller/ContractController.java:29-36`, `ContractServiceImpl.java:146-152`, `T/bookings/detail.html:349-352`; đáp ứng [S2 Đ38(1)(c), Đ29(2)], [S3 Đ12(3)] |
| Tiền cọc VNPay là hành vi xác nhận thứ hai, mã giao dịch in trong PDF | `T/pdf/rental-contract.html:183-187` |

## 2. Bảng khoảng cách

Mức độ: **bắt buộc** = có điều luật đặt ra nghĩa vụ, áp dụng khi vận hành thật; **nên có** = thực hành tốt hoặc làm tăng giá trị chứng cứ; **tuỳ chọn** = không luật nào đòi, chỉ dùng khi rủi ro đủ lớn.

| # | Yêu cầu | Hiện trạng | Nguồn | Mức độ | Tầng |
|---|---|---|---|---|---|
| G1 | Cho khách xem **đầy đủ, chính xác** nội dung hợp đồng trước khi giao kết | Modal chỉ có các bên, 3 chính sách và `terms_content` (`T/bookings/checkout.html:793-804`, `T/fragments/contract-terms.html:10-36`). Thời điểm hiệu lực, phí phát sinh, biên bản bàn giao là phụ lục chỉ xuất hiện trong PDF (`T/pdf/rental-contract.html:188,213-214,237`), mà PDF sinh sau khi đã đồng ý và đã cọc (`ContractServiceImpl.java:42-43,99-113`) | [S2 Đ38(1)(a), Đ21(7)], [S3 Đ12(1)] | bắt buộc | a, b |
| G2 | Hợp đồng có đủ nội dung luật định | Không có mã số doanh nghiệp (`rental-contract.html:80-82`). Không có quyền chấm dứt 30 ngày, quy trình khiếu nại, trách nhiệm bảo vệ thông tin, bất khả kháng: tìm trong PDF, modal, checkout và `database/seed.sql` đều không thấy. Rủi ro: thông tin thiếu cho khách quyền đơn phương chấm dứt trong 30 ngày, được hoàn phần chưa dùng | [S2 Đ37(1)(b,i,k,l), Đ38(2)-(4), Đ23(3)(g,i)] | bắt buộc | a |
| G3 | Công khai hợp đồng mẫu trên website trước khi khách đặt cọc | Toàn văn chỉ có ở checkout, sau đăng nhập (`J/config/WebConfig.java:34-35`). Trang xe chỉ có 3 dòng tóm tắt (`T/vehicles/detail.html:164-167`) | [S2 Đ26(3), Đ27], [S4 Đ405(1), Đ406(2)] | bắt buộc | a |
| G4 | Chính sách bảo mật; cơ chế đồng ý trước khi mở tài khoản | Không template nào có chính sách bảo mật hay điều khoản sử dụng. Form đăng ký không có checkbox (`T/auth/register.html:40-64`). Lưu ý không gộp đồng ý dữ liệu làm điều kiện hợp đồng [S2 Đ25(14)]; dữ liệu cần để thực hiện hợp đồng có thể xử lý không cần đồng ý [S11 Đ19(1)(d)] | [S3 Đ11(1)(b),(4)], [S6 Đ5], [S2 Đ16-17], [S11 Đ9] | bắt buộc | a |
| G5 | Giao kết tự động minh bạch, an toàn, **truy vết được** | Không có bảng audit nào. Chỉ có `accepted_at`, `issued_at`, `emailed_at`. Thao tác kích hoạt version không lưu người thực hiện (`ContractTemplateServiceImpl.java:88-100`), tải PDF không ghi log. Việc [S3 Đ13] áp dụng cho checkout loại này là cách đọc của tôi, xem câu hỏi mở | [S3 Đ13(2), Đ3(16)], [S1 Đ11(2)] | bắt buộc (cần xác nhận phạm vi) | a, b |
| G6 | Lưu giữ, truy cập được, cấp lại bản sao | Lưu tại `uploads/contracts` cục bộ (`FileUploadServiceImpl.java:28-29`), bị gitignore (`.gitignore:16`), không có backup. Mất file thì báo lỗi, không tái tạo được (`ContractServiceImpl.java:137-140`). Không có chính sách lưu trữ. Mức tối thiểu: đến khi hợp đồng hết hiệu lực và cấp bản sao trong 7 ngày làm việc [S2 Đ26(2)]; truy cập được ít nhất 3 năm (1 năm với doanh nghiệp siêu nhỏ và một số đối tượng) [S3 Đ16(2)(b,c)] | [S2 Đ26(2)], [S3 Đ16(2)], [S1 Đ13(1)] | bắt buộc | a, b |
| G7 | Áp dụng đúng điều khoản khách đã chấp nhận | **Hai lỗi thật.** (1) Trang chi tiết tính mốc huỷ theo version đang ACTIVE (`BookingController.java:454-461`), trong khi service dùng version đã chấp nhận, nên hiển thị có thể lệch với hoàn tiền thực. (2) `freeHours` mặc định 72, nuốt mọi exception, bỏ qua giá trị 0 (`BookingServiceImpl.java:311-318`), dù form và DB cho phép 0 (`J/dto/ContractTemplateFormDTO.java:36`, `schema.sql:172`) | [S4 Đ401(2)], [S2 Đ25(3)] | bắt buộc | a, c |
| G8 | Thông tin đưa ra phải chính xác | Modal ghi tài khoản "đã xác minh giấy phép lái xe" (`checkout.html:796`), nhưng `hasValidLicense` chỉ xét tồn tại và hạn (`J/service/impl/LicenseDocumentServiceImpl.java:60-63`), `LicenseDocument` không có trạng thái duyệt. `docs/PROJECT_ROADMAP.md:101` nói có duyệt/từ chối, không khớp code | [S2 Đ38(1)(a)] | nên có | a |
| G9 | Hợp đồng và nội dung công khai bằng tiếng Việt | Hợp đồng là tiếng Việt. Chính sách trên trang xe và thông báo lỗi clickwrap là tiếng Anh (`vehicles/detail.html:165-167`, `BookingCreateDTO.java:28`) | [S2 Đ23(2)], [S3 Đ11(2)], [S5 Đ6(1)] | bắt buộc với nội dung công khai | a |
| G10 | Thông báo nền tảng TMĐT trước khi vận hành | Việc thủ tục, không phải code. Nền tảng kinh doanh trực tiếp có đặt hàng trực tuyến phải được xác nhận thông báo | [S3 Đ3(3,8)], [S6 Đ23(1), Đ24] | bắt buộc | a |
| G11 | Phát hiện được sửa đổi nội dung | Không có hash cho PDF lẫn template. `docs/PROJECT_ROADMAP.md:235` ghi rõ quyết định **không** dùng chữ ký số hay mã băm. Luật không đòi hash, nhưng bản gốc điện tử cần toàn vẹn từ lúc khởi tạo [S1 Đ10(1)]. Dropbox Sign lưu hash PDF để so sánh [S20] | [S1 Đ10-11], [S20] | nên có | b |
| G12 | Giữ đúng văn bản đã đồng ý, không chỉ số version | Câu chữ PDF nằm trong code, không gắn version (`rental-contract.html:188,208,213-214`). Câu đồng ý cũng vậy (`checkout.html:568-569`). Deploy mới làm đổi văn bản của hợp đồng đã đồng ý nhưng chưa phát hành. PDF đọc dữ liệu sống lúc phát hành: GPLX mới nhất, biển số hiện tại (`ContractServiceImpl.java:183,192`). `PROJECT_ROADMAP.md:237` nói có `bookings.license_plate`, nhưng `schema.sql` không có cột này | [S1 Đ10(1), Đ11(2)], [S3 Đ13(2)] | nên có | b |
| G13 | Bản ghi chấp nhận đủ để tái dựng sự việc | Không lưu user agent, câu đồng ý, việc khách có mở modal hay không. Tick được mà chưa mở, dù câu ghi "đã đọc". IP được đưa vào DTO (`ContractServiceImpl.java:216`) nhưng không in ra PDF; version cũng không in, theo quyết định `PROJECT_ROADMAP.md:235`. Tham chiếu ngành: DocuSign ghi IP, sự kiện, mốc thời gian, envelope ID [S18]; Adobe ghi các mốc xem, ký [S19]; Dropbox Sign ghi cả user agent [S20] | [S1 Đ11(2), Đ13(1)(c)], [S18-S20] | nên có | b |
| G14 | Gắn được hành động với đúng người | Chỉ có phiên đăng nhập mật khẩu 30 phút (`J/service/impl/AuthServiceImpl.java:27-46`, `application.properties:15`). Không xác minh email, không OTP khi đồng ý. Luật không đòi OTP; attribution được xác định qua phương pháp đã thoả thuận hoặc mọi cách chứng minh [S1 Đ14(2)], [S13 §9(a)]. VNPay bù một phần | [S1 Đ14(2)], [S13 §9] | nên có | b |
| G15 | An toàn phiên khi chấp nhận | Không có CSRF token, vì chỉ dùng `spring-security-crypto` (`pom.xml:18-19`, `J/config/SecurityConfig.java:9-15`). SameSite=Lax giảm rủi ro (`application.properties:14`). Không bật cờ Secure cho cookie. IP lấy từ `getRemoteAddr` sẽ thành IP proxy nếu đặt sau reverse proxy | [S1 Đ11(2)] | nên có | b |
| G16 | Bất biến ở tầng DB | `schema.sql:156` ghi "immutable", nhưng chỉ service bảo đảm. Không trigger, không REVOKE; entity có setter, không `updatable=false` (`J/entity/Contract.java:25-29`, `J/entity/ContractTemplate.java:21-31`). Người có quyền DB sửa được mà không để dấu vết | [S1 Đ10(1), Đ11(2)] | nên có | b |
| G17 | PDF tự chứng minh toàn vẹn, nguồn gốc, thời gian | Không có chữ ký, seal hay timestamp. Chỉ dùng `openhtmltopdf` (`pom.xml:26`, `J/service/impl/OpenHtmlToPdfContractGenerator.java:48-54`). "Xác nhận điện tử bởi hệ thống DriveNow" chỉ là chữ in (`rental-contract.html:225`). Thời gian lấy từ đồng hồ server. Không phải PDF/A | [S1 Đ3(12,15), Đ22(3)], [S7 Đ24], [S14], [S15], [S16], [S17] | tuỳ chọn ở (a), nên có ở (b) | b |
| G18 | Giao bản lưu bền vững cho khách | Email là tuỳ chọn, lỗi thì chỉ ghi log, không tự thử lại, cần nhân viên bấm lại (`ContractServiceImpl.java:121-127`, `ContractController.java:43-61`). Khách vẫn tải được, nên nghĩa vụ [S2 Đ38(1)(c)] đã đạt. Email thành công là bản đối chứng độc lập giá trị nhất hiện có | [S2 Đ38(1)(c)] | nên có | b |
| G19 | Liên kết điều khoản dễ nhận biết | Link đậm, gạch chân, màu `#0f172a`, cỡ 12px (`src/main/resources/static/css/modules/booking.css:1241,1278-1291`). Án Mỹ coi gạch chân đơn thuần thường chưa đủ [S12a]; luật Việt Nam không có chuẩn cụ thể ngoài yêu cầu rõ ràng, dễ theo dõi [S5 Đ6] | [S12a], [S5 Đ6] | nên có | a, b |
| G20 | Giới hạn và ghi nhận người xem hợp đồng | Mọi staff xem được mọi hợp đồng (`ContractController.java:38-41`, `J/interceptor/RoleInterceptor.java:24`), đã có trong backlog C16 | [S11 Đ3(2),(4)], `PROJECT_ROADMAP.md:160` | nên có | b |
| G21 | Test các hành vi trên | Có test cho checkbox, chủ sở hữu PDF, phát hành lần hai. **Chưa có:** từ chối version lệch, phát hành lần đầu, lỗi email, ràng buộc DB trên Postgres thật, hai lỗi G7 | `BookingContractFlowTest.java`, `ContractControllerTest.java:33-70`, `ContractIssueTest.java:38` | nên có | c |

### Không cần làm dù nghe có vẻ "chuẩn"

| Việc | Vì sao không cần |
|---|---|
| Khách ký bằng chữ ký số công cộng | Hợp đồng thuê xe không thuộc diện luật buộc hình thức [S4 Đ117(2), Đ119]. NĐ 23 không có quy định riêng về hợp đồng điện tử [S7] |
| Đăng ký hợp đồng mẫu với Sở Công Thương | Danh mục 8 nhóm không có cho thuê xe [S8]. Vẫn phải công khai theo G3 |
| Bắt cuộn hết văn bản | Luật yêu cầu cung cấp nội dung và thời gian hợp lý [S2 Đ26(1), Đ38(1)], không đòi cuộn. Án Mỹ xét độ rõ của thông báo, không đòi người dùng đã đọc [S12b] |
| Dịch vụ chứng thực hợp đồng điện tử | Định nghĩa là dịch vụ bên thứ ba [S3 Đ3(15), Đ36]; tôi không thấy điều khoản nào buộc người bán phải dùng |
| Tuân thủ ESIGN, UETA, eIDAS | Không áp dụng cho giao dịch nội địa này, chỉ để tham khảo. eIDAS cũng không phủ nhận chữ ký đơn giản [S21 Đ25(1)] |

## 3. Khuyến nghị ưu tiên

Công sức: S dưới nửa ngày, M 1 đến 2 ngày, L từ 3 ngày trở lên. Ước lượng thô.

**P0: nên làm cho đồ án**

1. Sửa G7: thêm một chỗ duy nhất lấy `freeCancellationHours` từ `ContractService.getTermsOfBooking`, bỏ fallback 72 và `catch` rỗng, viết test. **S**
2. Sửa G8: đổi câu "đã xác minh" thành đúng sự thật. **S**
3. Thêm test version lệch và phát hành lần đầu (G21). **S**

**P1: để đạt tầng (a) khi vận hành thật**

4. Hiển thị trước khi đồng ý đúng bản hợp đồng sẽ phát hành, dựng từ cùng một template với dữ liệu booking (G1, G12). **M**
5. Bổ sung nội dung thiếu vào template và PDF, nên nhờ người có chuyên môn pháp lý rà (G2). **S** về code, thời gian soạn thảo tuỳ
6. Trang điều khoản và chính sách bảo mật công khai, checkbox khi đăng ký (G3, G4). **M**
7. Sao lưu `uploads/contracts`, ghi chính sách lưu trữ (G6). **S**. Thủ tục G10 làm ngoài code

**P2: để tiến tới tầng (b), theo thứ tự lợi ích trên công sức**

8. Bảng `contract_events` append-only: chấp nhận, mở modal, phát hành, gửi email, tải, kèm user agent và phiên bản câu đồng ý (G5, G13, G20). **M**
9. SHA-256 cho PDF và nội dung template, kiểm tra khi tải (G11). **S đến M**. Việc này đảo ngược quyết định ở `PROJECT_ROADMAP.md:235`, cần nhóm chốt. Hash lưu cùng DB chỉ phát hiện sai sót ngẫu nhiên, **không** chống được người có quyền DB; giá trị thật đến từ bản email hoặc timestamp bên ngoài.
10. Trigger chặn UPDATE, DELETE trên trường bằng chứng và template đã ACTIVE (G16). **S đến M**
11. Xác minh email, OTP khi chấp nhận, CSRF, cookie Secure, cấu hình IP sau proxy (G14, G15). **M đến L**
12. Outbox thử lại email (G18). **M**

**P3: tuỳ chọn, chỉ khi có giá trị tranh chấp đủ lớn**

13. Doanh nghiệp ký số vào PDF theo PAdES B-T với dấu thời gian RFC 3161 của tổ chức được cấp phép, hoặc B-LT nếu cần kiểm tra dài hạn; dùng PDF/A (G17). **L** và chi phí chứng thư.
14. Thay toàn bộ bằng nền tảng ký bên thứ ba hoặc dịch vụ chứng thực hợp đồng điện tử. **L**

## 4. Câu hỏi mở

1. `[S3 Đ13]` có áp dụng cho checkout một người bán không? Định nghĩa "hợp đồng tự động" [S3 Đ3(16)] rộng và CRMS giao kết không qua người, nên tôi coi là có, nhưng chưa thấy hướng dẫn chính thức.
2. Clickwrap thuộc "chữ ký điện tử" [S1 Đ3(11)] hay "hình thức xác nhận khác" [S1 Đ22(4)]? Không thấy văn bản nào trả lời.
3. Thời hạn lưu hợp đồng khi kèm chứng từ kế toán: NĐ 248 nêu tối thiểu 10 năm cho **tổ chức chứng thực** [S6 Đ41(4)], tôi chưa đọc luật kế toán nên không suy rộng cho người bán. Khi kiện đòi quyền lợi, thời hiệu là 3 năm từ ngày biết quyền bị xâm phạm [S4 Đ429], nên lưu đúng 3 năm từ ngày giao kết có thể chưa đủ.
4. Phí trễ 200% và mất cọc có vướng [S2 Đ25(12),(15)] không? Đây là câu hỏi nội dung, ngoài phạm vi kỹ thuật.
5. IP có phải dữ liệu cá nhân cơ bản theo mục "thông tin khác gắn với con người cụ thể" [S10 Đ3(11)] không? Nên xử lý như có.
6. Nhóm có giữ quyết định "không hash, không hiện version" ở `PROJECT_ROADMAP.md:235` không? Đây là quyết định trong roadmap, không phải ADR; `docs/adr/` chưa có quyết định nào về hợp đồng. Các ý ở G12 và P2 mục 9 mâu thuẫn với nó và cần được chốt rõ.

### Chưa xác minh được từ nguồn chính

- **NĐ 15/2026/NĐ-CP:** cổng Chính phủ xác nhận nghị định này sửa 7 nghị định, gồm NĐ 23/2025, để cắt giảm thủ tục hành chính [S9], nhưng tôi chưa đọc văn bản. Trang thuộc tính trên Công báo của NĐ 23 không hiện văn bản sửa đổi nào. "VBHN 17/2026" chỉ thấy trên site nhà cung cấp newca.vn, không dùng làm căn cứ. Các điều NĐ 23 tôi trích là bản gốc; nội dung sửa có vẻ là thủ tục, nhưng đó là suy đoán.
- **NĐ 55/2024:** đọc qua toàn văn đăng trên cổng Chính phủ, chỉ phần các điều cần dùng. Không tìm thấy văn bản sửa đổi, nhưng chưa khẳng định chắc chắn hiện trạng.
- **TT 42/2025/TT-BCT:** đọc qua thông cáo của Bộ Công Thương, không phải toàn văn. Thông cáo nói kế thừa danh mục QĐ 07/2024, không nói rõ có bãi bỏ quyết định đó không.
- **Mức xử phạt theo luật dữ liệu cá nhân:** không dùng. Một nguồn thứ cấp nhắc "NĐ 330/2026" nhưng tôi không xác minh được.
- **Adobe:** chỉ đọc được trang danh sách sự kiện. Không xác nhận được báo cáo có hash không. **DocuSign:** đã đọc phần mô tả nội dung Certificate, chưa xác nhận cơ chế niêm phong.
- **UETA:** đọc bản sao trên website Đại học Nevada, không lấy được từ Uniform Law Commission.
- **thuvienphapluat.vn** trả 403. Không điều nào trong ghi chú này dựa vào site đó hay các mirror khác.

## 5. Nguồn

Truy cập 2026-11-07 đến 2026-11-08. Luật và văn bản ghi là của cơ quan nhà nước; các nguồn ngành chỉ là **thực tiễn thị trường, không phải luật**.

**Việt Nam**

- [S1] Luật Giao dịch điện tử 20/2023/QH15, đọc theo văn bản hợp nhất 36/VBHN-VPQH gồm sửa đổi đến 01/7/2026. https://datafiles.chinhphu.vn/cpp/files/vbpq/2026/3/36-vbhn-vpqh.pdf ; tình trạng "còn hiệu lực": https://mst.gov.vn/van-ban-phap-luat/24987.htm
- [S2] Luật Bảo vệ quyền lợi người tiêu dùng 19/2023/QH15, hiệu lực 01/7/2024. https://datafiles.chinhphu.vn/cpp/files/vbpq/2023/7/luat19_2023.pdf
- [S3] Luật Thương mại điện tử 122/2025/QH15, hiệu lực 01/7/2026 (Đ40). https://congbao.chinhphu.vn/van-ban/luat-so-122-2025-qh15-468683/61714.htm ; bản ký: https://datafiles.chinhphu.vn/cpp/files/vbpq/2026/01/luat122.2025.qh15.pdf
- [S4] Bộ luật Dân sự 91/2015/QH13, đọc từ bản scan. https://datafiles.chinhphu.vn/cpp/files/vbpq/2016/01/91.signed.pdf (Đ116-123) ; https://datafiles.chinhphu.vn/cpp/files/vbpq/2016/01/91_.pdf (Đ400-406, Đ429)
- [S5] NĐ 55/2024/NĐ-CP, thay NĐ 99/2011. https://xaydungchinhsach.chinhphu.vn/toan-van-nghi-dinh-55--2024-nd-cp-quy-dinh-chi-tiet-mot-so-dieu-cua-luat-bao-ve-quyen-loi-nguoi-tieu-dung-119240521113013883.htm
- [S6] NĐ 248/2026/NĐ-CP, hiệu lực 01/7/2026; Đ52(3) chấm dứt NĐ 52/2013 và NĐ 85/2021, trừ chuyển tiếp Đ53 đến 30/6/2027. https://congbao.chinhphu.vn/van-ban/nghi-dinh-so-248-2026-nd-cp-469983/66872.htm
- [S7] NĐ 23/2025/NĐ-CP, Công báo 373+374. https://congbao.chinhphu.vn/thuoc-tinh-van-ban-so-23-2025-nd-cp-44261?cbid=55039
- [S8] Bộ Công Thương về TT 42/2025/TT-BCT. https://moit.gov.vn/tin-tuc/bo-cong-thuong-ban-hanh-thong-tu-ban-hanh-danh-muc-san-pham-hang-hoa-dich-vu-phai-dang-ky-hop-dong-theo-mau-dieu-kien-gi.html
- [S9] Cổng Chính phủ, chỉ đạo điều hành 15/1/2026, về NĐ 15/2026. https://baochinhphu.vn/chi-dao-dieu-hanh-cua-chinh-phu-thu-tuong-chinh-phu-ngay-15-1-2026-102260115180952133.htm
- [S10] NĐ 356/2025/NĐ-CP, hiệu lực 01/01/2026, thay NĐ 13/2023 (Đ42). https://congbaocdn.chinhphu.vn/180507251028987904/2026/1/17/356signed-1768638052103952849513.pdf
- [S11] Luật Bảo vệ dữ liệu cá nhân 91/2025/QH15, hiệu lực 01/01/2026. https://congbao.chinhphu.vn/van-ban/luat-so-91-2025-qh15-45578.htm

**Quốc tế (tham chiếu)**

- [S12a] *Berman v. Freedom Financial Network*, 30 F.4th 849 (9th Cir. 2022). https://www.govinfo.gov/content/pkg/USCOURTS-ca9-20-16900/pdf/USCOURTS-ca9-20-16900-0.pdf
- [S12b] *Meyer v. Uber Technologies*, 868 F.3d 66 (2d Cir. 2017). https://www.govinfo.gov/content/pkg/USCOURTS-ca2-16-02750/pdf/USCOURTS-ca2-16-02750-0.pdf
- [S12c] *Specht v. Netscape*, 306 F.3d 17 (2d Cir. 2002), bản CourtListener. https://www.courtlistener.com/opinion/779363/specht-v-netscape-communications-corporation/ . Cả ba án cùng khung: thông báo đủ rõ và hành vi đồng ý không mơ hồ.
- [S13] ESIGN, 15 USC 7001, 7006(5): https://www.law.cornell.edu/uscode/text/15/7001 ; UETA 1999 §9, §12, bản sao: https://scs.nevada.edu/wp-content/uploads/2020/07/FED_Act99.pdf
- [S21] eIDAS (EU) 910/2014, hợp nhất 18/10/2024, Đ3(10-12), Đ25, Đ26, Đ41. https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:02014R0910-20241018

**Tiêu chuẩn kỹ thuật**

- [S14] NIST FIPS 186-5 (02/2023). https://csrc.nist.gov/pubs/fips/186-5/final
- [S15] RFC 3161, timestamp trên hash, chứng minh dữ liệu tồn tại tại một thời điểm. https://www.rfc-editor.org/rfc/rfc3161.html
- [S16] ETSI EN 319 142-1 V1.2.1 (PAdES, xây trên ISO 32000-1, 4 mức B-B đến B-LTA). https://www.etsi.org/deliver/etsi_en/319100_319199/31914201/01.02.01_60/en_31914201v010201p.pdf
- [S17] ETSI EN 319 102-1 V1.4.1. https://www.etsi.org/deliver/etsi_en/319100_319199/31910201/01.04.01_60/en_31910201v010401p.pdf

**Thực tiễn ngành, không phải luật**

- [S18] DocuSign Certificate of Completion. https://support.docusign.com/s/document-item?language=en_US&bundleId=oeq1643226594604&topicId=gpa1578456339545.html
- [S19] Adobe Acrobat Sign audit report. https://helpx.adobe.com/sign/web/settings-configuration/global-settings/audit-report.html
- [S20] Dropbox Sign audit trail. https://help.dropbox.com/security/dropbox-sign-audit-trail-overview ; user agent: https://developers.hellosign.com/api/manual-reference-pages/glossary/security-compliance
