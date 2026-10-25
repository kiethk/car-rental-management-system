# CRMS — Backlog và hướng dẫn làm việc

Tài liệu này dành cho **dev hoặc AI được giao việc trong dự án**. Nó tổng hợp mọi lỗi, điểm cần cải tiến và luồng còn thiếu để hoàn thiện hệ thống. Cập nhật lần cuối: 2026-10-25.

---

## 0. ĐỌC TRƯỚC TIÊN (bắt buộc)

1. **Đây là danh sách việc cần làm, KHÔNG phải lệnh làm tất cả.** Người giao việc sẽ yêu cầu từng việc một theo mã (ví dụ "làm B18"). Chỉ làm đúng mục được yêu cầu. Thấy mục khác liên quan thì **nhắc lại và hỏi**, đừng tự sửa.
2. **Người giao việc không quản lý mọi luồng.** Phân luồng của nhóm:

   | Người | Luồng phụ trách |
   |---|---|
   | Kiệt | Tìm xe, đặt xe, cọc, gán xe, nhận/trả xe, thanh toán cuối, hủy/hoàn tiền; ghép các phần |
   | Phú | Đăng ký/đăng nhập, hồ sơ, nộp và duyệt GPLX, tài khoản khách/nhân viên, phân quyền |
   | Phúc (người giao việc) | Loại xe, xe, biển số, kiểm định; phần hợp đồng/điều khoản đã làm |
   | Trí | Khuyến mãi, đánh giá, sự cố |

   Mục thuộc luồng của người khác: vẫn làm được nếu được yêu cầu, nhưng **giữ thay đổi nhỏ nhất có thể** và nói rõ file nào đụng tới để người phụ trách biết.
3. **Đổi entity hoặc schema dùng chung** (bảng `bookings`, `payments`, `vehicles`, ...) phải được Kiệt đồng ý trước. Hỏi người giao việc, đừng tự quyết.
4. **Quy trình cho mỗi việc:** đọc code liên quan → (nếu việc không nhỏ) nói cách định sửa và chờ đồng ý → sửa → chạy test → báo kết quả bằng lời dễ hiểu. Người giao việc là sinh viên cần **hiểu từng dòng code**, nên đừng viết thứ quá phức tạp và hãy giải thích lý do.
5. **Không `git commit`, không `git push` trừ khi được yêu cầu rõ.** Không commit mật khẩu, file upload, `target/`, thư mục IDE. Không đưa lên GitHub khi chưa được xác nhận.
6. **Thiếu thông tin nghiệp vụ thì hỏi**, đừng đoán.
7. Trả lời bằng **tiếng Việt**, ngắn gọn, nói rõ file đã đổi và cách kiểm tra.

### Thầy giáo chấm theo 4 tiêu chí (mọi việc đều phải hướng tới)
- Code sạch, đúng convention Java, **đặc biệt interface và implementation** (lập trình theo interface, không phụ thuộc class cụ thể).
- Hệ thống đơn giản nhưng **chắc**; người viết phải nắm từng dòng.
- Giao diện tối ưu trải nghiệm người dùng.
- Nghiệp vụ đúng/phù hợp thực tế trong phạm vi đề tài.

---

## 1. Bối cảnh kỹ thuật

**Stack:** Java 21, Spring Boot 4.1.1, Spring MVC + Thymeleaf, Spring Data JPA (Hibernate), PostgreSQL 16 cài trực tiếp (**không dùng Docker**), Bootstrap 4 cho trang admin, Tailwind build sẵn (`drivenow.css`) cho trang khách. **Không dùng Spring Security** (xác thực bằng session + interceptor, chỉ dùng `spring-security-crypto` cho BCrypt). Không có React.

**Chạy dự án**
```bash
# 1. Tạo database car_rental_management trên PostgreSQL local (DBeaver hoặc psql)
# 2. Chạy lần lượt database/schema.sql (XÓA SẠCH mọi bảng rồi tạo lại) rồi database/seed.sql
# 3. Tạo src/main/resources/application-local.properties từ config/application-local.properties.example (file này bị git bỏ qua)
./mvnw spring-boot:run          # http://localhost:8081
./mvnw clean verify             # build + toàn bộ test, phải xanh
```
- `ddl-auto=validate`: entity phải **khớp đúng** `schema.sql`, sai là app không khởi động. Không bao giờ đổi sang `update/create`.
- Schema theo hướng **database-first**: sửa `database/schema.sql` + `database/seed.sql`, không viết file migration (cả nhóm reset DB rồi chạy lại). Đổi schema thì thêm ERD đề xuất vào `docs/proposed/*.md`; **không sửa** `docs/reference/erd_car.drawio` (bản gốc).
- Gửi email hợp đồng là **tùy chọn**: chỉ cần thêm `spring.mail.username` và `spring.mail.password` (Gmail App Password) vào file local. Không có thì app vẫn chạy, chỉ ghi log.

**Tài khoản mẫu** (mật khẩu `123456`): khách `customer@example.com` (GPLX đã duyệt), `customer2@example.com` (đã duyệt), `customer3@example.com` (GPLX chờ duyệt); nhân viên `admin@crms.com` (admin), `staff@crms.com` (staff). Sau khi chạy seed mới tự đăng ký thêm tài khoản (id bắt đầu từ 4).

**Thanh toán thử:** VNPay sandbox, thẻ NCB `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`. Không trừ tiền thật.

**Session:** các attribute `customerId`, `customerName`, `licenseStatus`, `employeeId`, `employeeName`, `employeeRole` (`admin`/`staff`). `AuthInterceptor` bảo vệ `/profile/**`, `/bookings/**`, `/licenses/**`; `RoleInterceptor` bảo vệ `/admin/**` (admin và staff đều vào được).

### Bản đồ code nhanh (`src/main/java/com/crms/mvc/`)
| Luồng | Controller | Service (interface → impl) |
|---|---|---|
| Đăng nhập/đăng ký/hồ sơ | `AuthController`, `ProfileController`, `EmployeePasswordController` | `AuthService`, `CustomerService`, `EmployeeService` |
| GPLX | `LicenseDocumentController` (cả khách và admin) | `LicenseDocumentService`, `FileUploadService` |
| Xe công khai | `VehicleController`, `HomeController` | `VehicleService` |
| Quản lý xe (admin) | `AdminVehicleController`, `AdminVehicleTypeController`, `AdminLicensePlateController`, `AdminInspectionController` | `VehicleService`, `VehicleTypeService`, `LicensePlateService`, `VehicleInspectionService` |
| Đặt xe | `BookingController` | `BookingService`, `SlotService`, `ContractTemplateService` |
| Thanh toán | `VnPayController` | `PaymentService`, `VnPayService`, `BookingFeeService` |
| Hợp đồng | `ContractController`, `AdminContractTemplateController` | `ContractService`, `ContractTemplateService`, `ContractDocumentGenerator`, `ContractMailService` |
| Quản lý booking (admin) | `AdminBookingController`, `AdminDashboardController` | `BookingService` |

Template ở `src/main/resources/templates/` (`fragments/`, `admin/`, `bookings/`, `vehicles/`, `pdf/`, `mail/`). Test ở `src/test/java/`.

---

## 2. Quy ước code (BẮT BUỘC cho mọi thay đổi)

**Phân tầng:** `controller → service (interface) → service/impl → repository → entity`. DTO nằm trong `dto/`. Mẫu để bắt chước: `LicensePlateService`/`LicensePlateServiceImpl`, `ContractTemplateService`/`ContractTemplateServiceImpl`.
- Controller **không** inject repository (còn vi phạm ở C4; code mới không được vi phạm).
- Service luôn có interface; inject bằng constructor, khai báo biến theo **kiểu interface**. `@Transactional` ở class impl (`readOnly = true` cho hàm đọc).
- Code mới trả **DTO** cho view, không trả entity (`open-in-view=false` nên truy cập lazy ngoài transaction sẽ lỗi).
- Form: Bean Validation (`@NotBlank`, `@Size`, ...) trên DTO + `BindingResult`; **luôn kiểm tra lại ở server**, không tin JS.
- Lỗi nghiệp vụ: `IllegalArgumentException` (dữ liệu vào sai), `IllegalStateException` (vi phạm quy tắc nghiệp vụ), `NoSuchElementException` (không tìm thấy). Controller bắt và hiển thị bằng flash message.
- Tiền dùng `BigDecimal`, tính ở **service**, không tính trong template hay JS.
- **Không dùng chuỗi trạng thái rải rác trong code mới**: dùng enum/hằng (mẫu: `ContractTemplateStatus` với `@Enumerated(EnumType.STRING)`). Code cũ còn dùng chuỗi chữ thường (`"confirmed"`, `"rented"`...), xem C7.
- Danh sách admin có tìm kiếm/sắp xếp/phân trang: dùng đúng mẫu `PageQueryDTO` + `PageNavDTO` + `fragments/pagination.html` (xem `AdminVehicleController.list`, `VehicleServiceImpl.searchForAdmin`, `VehicleRepository.searchForAdmin`). Nút thao tác trong danh sách gửi kèm `returnTo` để quay về đúng trang.
- Chính sách (cọc/hủy/trễ) hiển thị ở nhiều trang: câu chữ viết trực tiếp trong từng trang, **con số lấy từ DB** (bảng `contract_templates`), và mọi chỗ hiển thị chính sách phải có thuộc tính `data-policy="deposit|cancellation|lateReturn"` để tìm bằng Ctrl+Shift+F.
- Có test cho logic mới (JUnit 5 + Mockito, `@WebMvcTest` với `@MockitoBean`). `./mvnw clean verify` phải xanh trước khi báo xong.
- Giữ nguyên kiểu xuống dòng (CRLF/LF) và UTF-8 của từng file khi sửa.
- Ngôn ngữ giao diện hiện là **tiếng Anh**, riêng hợp đồng PDF và nội dung điều khoản là **tiếng Việt**. Chưa có quyết định đổi, hỏi nếu cần.

### Cạm bẫy đã gặp (đọc để khỏi mất thời gian)
1. **`drivenow.css` là file Tailwind build sẵn**: class nào không có trong file thì *im lặng không có tác dụng* (ví dụ `max-w-3xl`, `rounded-2xl`, `bg-slate-900/60`, `mt-0.5`). Cần style mới thì viết CSS thường vào `static/css/app.css`. Ngoài ra `drivenow.css` đặt `min-height: 2.75rem` cho **mọi `<button>`**, nên nút nằm trong dòng chữ phải ghi đè.
2. **Lazy loading:** `open-in-view=false`. Dùng `JOIN FETCH`/`@EntityGraph` hoặc map sang DTO ngay trong service. `Vehicle.currentPlates` có `@BatchSize` để tải biển số cả trang bằng một câu truy vấn.
3. **Thứ tự flush của Hibernate:** INSERT chạy trước UPDATE. Khi ràng buộc DB cần UPDATE xảy ra trước (một biển hiện tại/xe, một điều khoản ACTIVE) phải `saveAndFlush` bản ghi cũ trước (xem `LicensePlateServiceImpl.changePlate`, `ContractTemplateServiceImpl.activate`).
4. **Thymeleaf 3.1** không có `#request`/`#session`; dùng biến `session.xxx` và `param.xxx`.
5. **Múi giờ:** `CarRentalApplication.main` đặt JVM mặc định là UTC (xem C8).
6. **Sự kiện sau commit:** sinh PDF hợp đồng chạy trong `ContractIssuingListener` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`), lỗi ở đó không được làm hỏng thanh toán.
7. **Khi test bằng DB thật:** dùng một database tạm riêng (không đụng `car_rental_management` của người dùng), xong thì **tắt mọi server đã bật và xóa database tạm**. Người dùng tự khởi động app của mình.

---

## 3. Trạng thái hiện tại

**Đã có và chạy được:** đăng ký/đăng nhập/hồ sơ/đổi mật khẩu; nộp GPLX (ảnh lưu `uploads/licenses`, chỉ chủ và nhân viên xem được) và nhân viên duyệt/từ chối; quản lý loại xe, xe, **biển số theo lịch sử** (1-N, `valid_from`/`valid_to`), kiểm định, kèm tìm kiếm/sắp xếp/phân trang; danh sách xe công khai có lọc và sắp xếp (giá, tên A→Z, Z→A); tìm xe theo khung giờ; checkout có **clickwrap**; tạo booking `pending_payment`; cọc qua VNPay sandbox; **hợp đồng PDF** sinh sau khi cọc và gửi email (tùy chọn); trang booking của khách; admin: danh sách booking theo trạng thái, gán xe, nhận xe (check-in), trả xe (check-out); quản lý **phiên bản điều khoản** (`/admin/contract-templates`); dashboard thống kê cơ bản. Tổng cộng 64 test.

**Chưa có ở bất kỳ nhánh nào:** quản lý khuyến mãi, đánh giá, sự cố/phạt, quản lý tài khoản (menu admin có link nhưng trả 404), hoàn tiền khi hủy.

---

## 4. Lỗi và cải tiến cần xử lý

Cột "Cỡ": **S** nhỏ (dưới 1 giờ), **M** vừa, **L** lớn. "Luồng" cho biết ai phụ trách (xem mục 0).

### A. Bảo mật
| Mã | Vấn đề | Vị trí | Hướng sửa | Cỡ | Luồng |
|---|---|---|---|---|---|
| **A1** | Endpoint xác nhận cọc **không qua VNPay**, không kiểm tra chủ đơn: ai đăng nhập cũng "xác nhận cọc" miễn phí | `BookingController.processDeposit` (`POST /bookings/{id}/pay-deposit`) | **Xóa endpoint** (mọi xác nhận cọc đi qua `/payment/vnpay-return`). Nhóm **tạm giữ lại để test**, người giao việc sẽ báo khi nào xóa: nên xóa trước ngày demo. Nhân tiện: `/bookings/{id}/vnpay-payment` đang nhận cả GET lẫn POST, chỉ nên POST | S | Kiệt |
| **A2** | **Tài khoản admin viết cứng** (`admin`/`admin@gmail.com`/`admin@crms.com` với mật khẩu `admin123`/`123456`/`admin`), tự tạo nhân viên nếu chưa có | `AuthServiceImpl.login` | Xóa cả khối này; seed đã có `admin@crms.com` với hash bcrypt | S | Phú |
| **A4** | VNPay trả về: không đối chiếu số tiền (`vnp_Amount` = tiền × 100) với tiền cọc; không lưu `vnp_TransactionNo`; nhãn thanh toán vẫn là `VIETQR`; log in cả URL và dữ liệu băm | `VnPayController.handleVnPayReturn`, `PaymentServiceImpl`, `Payment` (default `"VIETQR"`), `schema.sql` (`payment_method DEFAULT 'VIETQR'`), `VnPayServiceImpl` | Kiểm tra số tiền + mã `00` + booking còn `pending_payment`; lưu mã giao dịch thật; phương thức `VNPAY`; bỏ log nhạy cảm | M | Kiệt |
| **A5** | Khóa sandbox VNPay là giá trị mặc định trong `application.properties` | `application.properties` | Chỉ là khóa test công khai; thật sự deploy thì lấy từ biến môi trường. Ghi chú trong README | S | Kiệt |
| **A6** | **Không có CSRF token** cho form POST (vì không dùng Spring Security) | toàn bộ form | Hạn chế đã biết. Chỉ làm khi cả nhóm đồng ý (thêm token qua interceptor, hoặc dùng Spring Security) | L | Cả nhóm |

### B. Nghiệp vụ
| Mã | Vấn đề | Vị trí | Hướng sửa | Cỡ | Luồng |
|---|---|---|---|---|---|
| **B1** | **SLOT dùng sai ý nghĩa**: mỗi booking tạo một Slot mới với `capacity=1`; cột `capacity`/`status` của slot không có tác dụng. Theo đề tài SLOT là khung giờ thuê của một loại xe | `SlotServiceImpl.createSlotForBooking`, `BookingServiceImpl.createBooking` | **Cần thảo luận với Kiệt và thầy trước**, chưa tự làm | L | Kiệt |
| **B2** | **Xe trống bị xét theo trạng thái hiện tại** của xe cho một khung giờ tương lai: xe đang cho thuê hôm nay không đặt được cho tuần sau | `SlotServiceImpl.findAvailableVehiclesInTimeRange` (lọc `findByStatusWithDetails("available")`) | Xe trống = không có booking (confirmed/active/pending còn hạn) chồng giờ và không nằm trong lịch kiểm định/bảo trì; trạng thái chỉ để loại xe `maintenance` | M | Kiệt |
| **B3** | **Đặt trùng xe**: tạo booking tự chọn `availableVehicles.get(0)` (hai người cùng lúc có thể trùng); `assignVehicle` chỉ kiểm tra trạng thái và loại xe, không kiểm tra chồng giờ; trang checkout cho khách **chọn xe cụ thể** (`bookingForm.vehicleId`) nhưng server **bỏ qua** | `BookingServiceImpl.createBooking`/`assignVehicle`, `BookingCreateDTO.vehicleId`, `checkout.html` | Kiểm tra chồng giờ khi tạo và khi gán; ràng buộc DB `EXCLUDE USING gist (vehicle_id WITH =, tstzrange(pickup_time, scheduled_end_time) WITH &&)` cho các booking còn hiệu lực (`btree_gist` đã cài sẵn); tôn trọng xe khách chọn hoặc bỏ lựa chọn đó khỏi giao diện | M–L | Kiệt |
| **B4** | **Cắt giờ lẻ** (`Duration.toHours()` làm tròn xuống) cho tiền thuê và phí trễ; phí trễ tối thiểu 1 giờ, không có thời gian ân hạn | `BookingController` (2 chỗ), `BookingServiceImpl.createBooking`/`checkOut`, `SlotServiceImpl.searchAvailableVehicleTypes`, `ContractServiceImpl.buildDocument` | Chốt quy tắc với nhóm (làm tròn lên theo giờ bắt đầu; ân hạn khi trả trễ), cài **ở một nơi** (xem B5) | M | Kiệt |
| **B5** | **Công thức giá/cọc lặp ở 4 nơi, không nhất quán**: checkout, `SlotServiceImpl`, `BookingServiceImpl`, JS trong `checkout.html`; `VehicleDTO.estimatedDeposit` = giá × 12 (sai) | các file trên + `VehicleDTO` | Tạo interface `PricingService` (nhận khoảng thời gian, giá/giờ, mã giảm giá, tỉ lệ cọc → trả `PriceQuoteDTO`) dùng chung; JS chỉ hiển thị số do server trả; bỏ/sửa `estimatedDeposit` | M | Kiệt |
| **B6** | **Khuyến mãi**: chỉ kiểm tra `quantity > 0`; không kiểm tra `start_date`/`end_date`, không trừ `quantity`, mã sai bị bỏ qua im lặng | `BookingServiceImpl.createBooking` | Làm cùng E1 | M | Trí |
| **B7** | **Hủy booking không hoàn cọc** dù hợp đồng hứa "hủy trước X giờ hoàn 100% cọc" (`free_cancellation_hours`) | `BookingServiceImpl.cancelBooking` | Xem E5 | M | Kiệt |
| **B8** | **Trả xe/thanh toán cuối**: khoản `FINAL` tự đặt `paid` ngay, phương thức vẫn `VIETQR`; không xử lý cọc lớn hơn tổng tiền; không ghi số km/nhiên liệu (`CheckOutDTO.returnNotes` không dùng); xe trả xong tự thành `available` kể cả khi hư hỏng | `BookingServiceImpl.checkOut`, `PaymentServiceImpl.processSettlement` | Xem E6 | M | Kiệt |
| **B9** | **Hủy đơn pending quá 15 phút được làm trong các hàm đọc** (`getMyBookings`, `getBookingDetail`, `getAllBookings`, `createBooking`): GET có tác dụng phụ, chỉ chạy khi có người xem | `BookingServiceImpl`, `BookingRepository.cancelExpiredPendingBookings` | Dùng `@Scheduled` (thêm `@EnableScheduling`) chạy mỗi phút; các hàm đọc không sửa dữ liệu nữa | S–M | Kiệt |
| **B10** | **GPLX**: chỉ cần *từng* có bản đã duyệt là "verified"; không xét ngày hết hạn so với ngày trả xe | `LicenseDocumentServiceImpl.isCustomerVerified` | Yêu cầu bản đã duyệt mới nhất còn hạn đến hết thời gian thuê; báo rõ ở checkout | S | Phú |
| **B12** | **Kiểm định**: ghi kết quả được mà chưa bắt đầu và ghi nhiều lần; đặt lịch được cho xe đang cho thuê hoặc có booking chồng giờ | `VehicleInspectionServiceImpl.startInspection`/`recordResult`/`schedule` | Chỉ cho `scheduled → passed/failed` một lần; chặn đặt lịch khi trùng booking | S–M | Phúc |
| **B13** | **Đánh giá giả** "4.8 sao, 120 đánh giá" viết cứng | `SlotServiceImpl.searchAvailableVehicleTypes`, `VehicleTypeSearchResultDTO` (mặc định 4.7/120) | Thay bằng dữ liệu thật sau E2 | S | Trí |
| **B14** | **Thiếu kiểm tra dữ liệu**: mật khẩu không có độ dài tối thiểu; `VehicleService.updateStatus` và form xe nhận chuỗi trạng thái bất kỳ (tạo xe được với trạng thái `rented`) | `CustomerRegisterDTO`, `PasswordChangeDTO`, `VehicleServiceImpl`, `VehicleFormDTO` | `@Size(min=8)`; chỉ cho `available`/`maintenance` ở form (`rented` do check-in đặt) | S | Phú, Phúc |
| **B15** | **Mã giảm giá giả trong JS** của `checkout.html` (`WELCOME10`, `FIRST10`, `GIAM100K` viết cứng) không liên quan bảng `promotions` | `checkout.html` (`applyPromoCode`) | Làm cùng E1: gọi server để kiểm tra mã | S–M | Trí |
| **B17** | **Giờ hoạt động của bãi xe 07:00–22:00** (có trong đề tài) chưa được áp dụng; form tìm xe ghi "Earliest: 06:00" | `fragments/rental-search-form.html`, `BookingServiceImpl` | Thêm cột giờ mở/đóng cửa (và phí qua đêm) vào `contract_templates`, kiểm tra khi đặt xe. **Chưa làm, người giao việc sẽ bổ sung sau** | M | Kiệt + Phúc |
| **B18** | **Danh sách booking của admin luôn rỗng**: `status=all` bị coi là một trạng thái thật | `AdminBookingController.listBookings`, `BookingServiceImpl.getAllBookings` | Coi `all` là không lọc; nhân tiện thêm tìm kiếm + phân trang theo mẫu ở mục 2 | S | Kiệt |
| **B19** | **Trạng thái GPLX trong header bị cũ**: lưu trong session, chỉ cập nhật ở trang checkout; duyệt xong khách vẫn thấy "Pending" đến khi đăng nhập lại | `GlobalSessionAdvice`, `BookingController.checkout`, `LicenseDocumentController.customerLicense` | Làm mới `licenseStatus` mỗi lần vào trang GPLX hoặc tính lại khi cần | S | Phú |
| **B20** | Sửa biển nhập sai chính tả sẽ tạo thêm một dòng lịch sử | `LicensePlateService.changePlate` | (Tùy chọn) thêm thao tác "sửa lỗi nhập" riêng | S | Phúc |

### C. Cấu trúc và convention (làm **kèm** khi chạm tới file đó, không làm thành một đợt riêng)
| Mã | Vấn đề | Hướng sửa | Cỡ |
|---|---|---|---|
| **C1** | `new BCryptPasswordEncoder()` trong `AuthServiceImpl`, `CustomerServiceImpl`, `EmployeeServiceImpl` | Khai báo một `@Bean PasswordEncoder` trong config rồi inject (kiểu interface) | S |
| **C2** | `VnPayService.createDepositPaymentUrl(Booking, HttpServletRequest)` kéo tầng web vào service; `VnPayConfig` chứa các hàm static | Truyền `String clientIp`; chuyển hàm tiện ích ra class riêng | S |
| **C3** | Service trả entity ra view; `@Transactional` đặt trên hàm controller (`VehicleController.detail`, `VnPayController`); `LicenseDocument.customer` EAGER để chữa cháy | Service trả DTO; bỏ `@Transactional` khỏi controller | M |
| **C4** | Controller gọi thẳng repository: `AdminBookingController`, `AdminDashboardController`, `BookingController`, `HomeController`, `VehicleController`, `VnPayController` | Thêm hàm vào service tương ứng | S–M mỗi nơi |
| **C5** | `VehicleService.findById(id)` tìm *loại xe* theo id rồi mới tìm *xe* theo cùng id: **trùng id sẽ ra nhầm**; `VehicleDTO` dùng cho cả loại xe lẫn xe vật lý | Tách `VehicleTypeDTO`/`VehicleDTO` và hai hàm tìm riêng | M |
| **C6** | Controller/service chưa tách hợp lý: `BookingController` quá lớn (tìm, checkout, cọc, lịch sử, hủy); hai nơi xử lý cọc; `LicenseDocumentController` lẫn endpoint khách và admin; `EmployeePasswordController` tách rời; phân tích ngày giờ bằng 6 tham số tùy chọn với `catch (Exception ignored)` | Tách theo vai trò/nghiệp vụ; gom tham số ngày giờ vào một DTO có validation | M–L |
| **C7** | Trạng thái là chuỗi chữ thường khắp nơi (`Booking`, `Vehicle`, `Payment`, `LicenseDocument`, `Promotion`, `VehicleInspection`, `Issue`, `Slot`, `Employee.role`) | Chuyển dần sang enum với `AttributeConverter` (giữ giá trị chữ thường trong DB để khớp `CHECK`). Đụng schema dùng chung nên **cần Kiệt**, làm từng entity một | L |
| **C8** | **Múi giờ**: trang web hiển thị giờ UTC (khách chọn nhận xe 09:00, trang hiện 02:00); `ZoneOffset.ofHours(7)` viết cứng ở `BookingController` (2 chỗ), `BookingServiceImpl.createBooking`, `VehicleInspectionServiceImpl` (3 chỗ). PDF hợp đồng thì đã đổi đúng về giờ Việt Nam | Một hằng/bean `ZoneId` `Asia/Ho_Chi_Minh` dùng chung; đổi giờ khi map sang DTO hoặc có formatter cho template. **Hiển thị sai giờ là lỗi nhìn thấy ngay, nên ưu tiên cao** | M |
| **C9** | Xử lý lỗi không thống nhất: `GlobalExceptionHandler` biến mọi `Exception` thành 500; controller bắt `Exception` và in `e.getMessage()` cho người dùng; service lúc trả `boolean`/`null`, lúc ném exception | Exception nghiệp vụ riêng (ví dụ `BusinessRuleException`, `ResourceNotFoundException`) xử lý ở `GlobalExceptionHandler` với mã HTTP đúng | M |
| **C10** | `CustomerServiceImpl`, `EmployeeServiceImpl`, `LicenseDocumentServiceImpl`, `AuthServiceImpl` thiếu `@Transactional`; `findByEmailIgnoreCase` trả `null` thay vì `Optional` | Bổ sung, đổi sang `Optional` | S |
| **C11** | Session: tên attribute là chuỗi rải rác; mọi controller tự kiểm tra lại `customerId` dù đã có interceptor; `AuthInterceptor` cho nhân viên vào route của khách; header dùng `session.customerEmail` nhưng **không nơi nào set** | Lớp hằng `SessionKeys`, một cách lấy "người dùng hiện tại" dùng chung; bổ sung/ bỏ `customerEmail` | M |
| **C12** | Hai lớp `WebMvcConfigurer` (`WebConfig`, `WebMvcConfig`) | Gộp thành một | S |
| **C13** | Code trùng/chết: `getDisplayName` lặp ở 3 lớp; `resolveImageUrl` lặp (`BookingServiceImpl`, `BookingController`); map `BookingSummaryDTO` viết hai lần; `FileUploadServiceImpl` lặp save/load; không dùng: `VehicleAssignDTO`, `CheckOutDTO.returnNotes`, `VehicleService.search` và vài hàm repository | Dọn dần khi chạm tới | S mỗi nơi |
| **C14** | Hiệu năng: tìm xe trống chạy một truy vấn cho mỗi loại xe; `searchWithFilter` nạp hết loại xe rồi lọc trong bộ nhớ; dashboard đếm bằng cách nạp cả danh sách | Truy vấn đếm/lọc ở DB | S–M |
| **C15** | Test còn thiếu cho: tạo/hủy/trả xe, thanh toán, `VnPayController`, GPLX, kiểm định | Viết test cùng với mỗi việc ở trên | liên tục |
| **C16** | Phân quyền admin/staff chưa nhất quán: `RoleInterceptor` coi hai vai trò như nhau; chỉ `AdminContractTemplateController` tự kiểm tra `isAdmin` | Chốt hành động chỉ admin được làm (quản lý tài khoản, điều khoản, xóa xe...) và áp dụng một cách thống nhất | M |

### D. Giao diện / trải nghiệm
| Mã | Vấn đề | Ghi chú |
|---|---|---|
| **D1** | Trộn hai hệ CSS: class Bootstrap 4 ở trang admin, tiện ích kiểu Tailwind ở trang khách | Không cần thống nhất toàn bộ; ưu tiên chỉnh **hành trình khách chính** (tìm xe → đặt → cọc → booking của tôi) |
| **D2** | `layouts/base.html` và `layouts/admin-base.html` không trang nào dùng; mỗi trang tự chép `<head>` | Dùng layout hoặc xóa |
| **D3** | Template quá lớn: `checkout.html`, `deposit.html`, `rental-search-form.html` (lịch tự viết bằng JS) | Chia fragment; cân nhắc `datetime-local`. Ưu tiên thấp |
| **D4** | Nhiều `style="..."` và `!important`; badge trạng thái lặp ở từng trang; sidebar nhận `activeNav` ở model trong khi hướng dẫn UI ghi tham số `active` | Gom thành fragment/lớp CSS khi sửa trang |
| **D5** | Cạm bẫy `drivenow.css` build sẵn (mục 2): các trang khác có thể đang bị hỏng ngầm | Khi thấy giao diện lệch, kiểm tra class có tồn tại trong `drivenow.css` không |
| **D6** | Danh sách admin khác chưa có tìm kiếm/sắp xếp/phân trang: booking, kiểm định, theo dõi biển số, GPLX chờ duyệt | Dùng đúng mẫu ở mục 2 |

---

## 5. Luồng còn thiếu (đặc tả tối thiểu)

### E1. Khuyến mãi — Trí (kèm B6, B15)
- Admin `/admin/promotions` (menu đã có link `active='promotions'`): danh sách có tìm kiếm/phân trang; tạo, sửa, xóa. Bảng `promotions`: `code` (duy nhất, không phân biệt hoa thường), `discount_type` (`percent`|`fixed`), `discount_value` (percent 1–100), `start_date`, `end_date`, `quantity`.
- Mã đã gắn vào booking thì không xóa được (cho hết hạn thay vì xóa).
- Khách ở trang checkout bấm "Apply": gọi **server** kiểm tra (mã tồn tại, hôm nay nằm trong `start_date..end_date`, `quantity > 0`) và trả số tiền giảm cho khoản thuê hiện tại, hoặc thông báo lỗi rõ ràng. **Xóa mã giả trong JS.**
- Tạo booking kiểm tra lại mã và **trừ `quantity` nguyên tử** (`UPDATE ... SET quantity = quantity - 1 WHERE promotion_id = ? AND quantity > 0`); hoàn lại khi booking bị hủy/hết hạn (cần chốt với Kiệt thời điểm trừ). Số tiền tính qua `PricingService` (B5).
- Seed đã có 2 mã mẫu (`WELCOME10`, `GIAM100K`).

### E2. Đánh giá — Trí (kèm B13)
- Khách chỉ đánh giá được booking `completed` **của chính mình**, **một lần** (`reviews.booking_id` UNIQUE), `rating` 1–5, `comment` tối đa 1000 ký tự. Form đặt ở trang chi tiết booking.
- Trang chi tiết loại xe hiển thị điểm trung bình và danh sách đánh giá (qua `booking → slot → vehicleType`); thẻ xe ở trang tìm kiếm dùng điểm thật (thay số giả). Chưa có đánh giá thì ghi "Chưa có đánh giá", không hiện số giả.
- Entity/repository `Review` đã có sẵn.

### E3. Sự cố / phạt — Trí
- Admin `/admin/issues`: nhân viên ghi nhận sự cố cho một xe (có thể kèm booking): `issue_type` (ví dụ SCRATCH, DAMAGE, ACCIDENT, CLEANING), `subject`, ảnh bằng chứng tải lên (`evidence_url`, kiểm tra loại file và dung lượng như `FileUploadService`), `status` `open → in_progress → resolved`; danh sách có lọc theo trạng thái, tìm kiếm, phân trang.
- Sự cố có chi phí thì tạo `booking_fees` (`fee_type` `DAMAGE_COMPENSATION`, `CLEANING_FEE`..., gắn `issue_id`) để cộng vào thanh toán cuối (E6). Sự cố nặng có thể chuyển xe sang `maintenance` (kiểm tra không phá booking sắp tới, B2/B3).
- Entity `Issue`, `BookingFee`, `BookingFeeService` đã có.

### E4. Quản lý tài khoản — Phú
- `/admin/accounts` (**chỉ admin**, xem C16): danh sách khách (chỉ xem) và nhân viên có tìm kiếm/phân trang; tạo nhân viên (admin/staff) với mật khẩu ban đầu (băm BCrypt, dùng `PasswordEncoder` ở C1), đổi vai trò, đặt lại mật khẩu. Email phải duy nhất trên cả hai bảng `customers` và `employees` (đã có kiểm tra khi đăng ký, giữ nguyên).
- Schema chưa có cột `active`: muốn khóa tài khoản phải thêm cột (đổi schema dùng chung, hỏi Kiệt).
- Áp dụng B14 (độ dài mật khẩu tối thiểu).

### E5. Hủy booking và hoàn cọc — Kiệt (B7)
- Khách hủy được booking `pending_payment` và `confirmed`. Với `confirmed`: nếu `now ≤ pickup_time − free_cancellation_hours` (lấy từ phiên bản điều khoản **khách đã đồng ý**, `ContractService.getTermsOfBooking`) thì hoàn 100% cọc, ngược lại cọc bị giữ.
- Hoàn tiền: tạo bản ghi `payments` loại `REFUND` (trạng thái `pending` cho nhân viên xử lý, hoặc `paid` tự động trong môi trường sandbox; chốt với Kiệt). Ghi `cancelled_at`; xe được thả (booking không còn chiếm xe).
- Giao diện hiển thị trước khi xác nhận hủy: "Bạn sẽ được hoàn X đ" hoặc "Cọc không được hoàn".

### E6. Trả xe và thanh toán cuối — Kiệt (B8)
- Tiền còn lại = tổng tiền − cọc + phí trễ + phí phát sinh (`booking_fees`); nếu cọc lớn hơn tổng thì **hoàn phần chênh** (`REFUND`).
- Ghi phương thức thu tiền tại quầy (tiền mặt/chuyển khoản) thay vì tự đặt `VIETQR`; ghi số km và ghi chú khi trả xe (cập nhật `vehicles.odometer`); nếu xe có sự cố thì cho chuyển sang `maintenance` thay vì tự `available`.
- Phí trễ tính theo quy tắc ở B4 và hệ số trong điều khoản khách đã đồng ý.

---

## 6. Thứ tự ưu tiên đề xuất (chỉ là gợi ý, người giao việc quyết định)

1. **Sửa nhanh, dễ bị thấy khi demo:** B18, C8, ẩn hoặc làm các link 404 trên menu admin, A2, A4, xóa A1 trước ngày demo.
2. **Nghiệp vụ lõi của thuê xe:** B2 + B3 (kèm `PricingService` cho B4/B5), rồi E5 (B7), E6 (B8), B9.
3. **Luồng còn thiếu:** E1 (kèm B6, B15), E2 (kèm B13), E3, E4. **Hỏi Trí và Phú** đã làm gì trước khi bắt đầu.
4. **Dọn code kèm theo** khi chạm tới file: C1, C4, C9, C13...
5. **Hoãn lại:** C7 (đổi hết sang enum, cần Kiệt), D3, B1 (cần thảo luận), B17, A6.

---

## 7. Hoàn thành một việc nghĩa là

- [ ] Chỉ làm đúng mục được giao; mục liên quan phát hiện thêm thì đã **báo lại**, chưa tự sửa.
- [ ] Code theo mục 2 (interface + impl, DTO, không để controller gọi repository, không chuỗi trạng thái mới rải rác, kiểm tra dữ liệu ở server).
- [ ] Đổi schema thì sửa cả `schema.sql` và `seed.sql`, entity khớp (app khởi động được với `validate`), có ERD đề xuất trong `docs/proposed/`, đã hỏi Kiệt nếu là bảng dùng chung.
- [ ] Có test cho logic mới; `./mvnw clean verify` xanh.
- [ ] Đã thử thật trên database tạm (nếu việc liên quan DB hoặc giao diện), **đã tắt server và xóa database tạm**.
- [ ] Giao diện mới dùng class có trong `drivenow.css` hoặc CSS thường trong `app.css`.
- [ ] Báo lại bằng tiếng Việt: đã đổi file nào, vì sao, cách kiểm tra, còn gì chưa làm; cập nhật README nếu thay đổi cách chạy hoặc quy ước.
- [ ] Chưa commit/push nếu chưa được yêu cầu.

---

## 8. Quyết định ĐÃ CHỐT (đừng đảo ngược nếu không được yêu cầu)

- **Hợp đồng:** điều khoản có phiên bản trong `contract_templates` (mỗi dòng một phiên bản bất biến; chỉ bản `DRAFT` sửa được; tối đa một `ACTIVE`). Số liệu nghiệp vụ (`deposit_rate`, `late_fee_multiplier`, `free_cancellation_hours`) là **cột có kiểu**, đoạn quyền/nghĩa vụ là `terms_content`. Khách chấp thuận bằng checkbox (clickwrap): lưu `accepted_at`, `accepted_ip`, phiên bản. PDF chỉ sinh **sau khi cọc thành công**, lưu ở `uploads/contracts/` (không công khai, tải qua `ContractController` có kiểm tra quyền), gửi email nếu đã cấu hình. **Không** dùng chữ ký số hay mã băm. Bảng `contracts` không có cột trạng thái (hiệu lực theo `bookings.status`). Không hiển thị "phiên bản" cho khách, chỉ admin thấy.
- **Chính sách:** câu chữ viết trực tiếp ở từng trang (không dùng `messages.properties`), con số lấy từ DB, có `data-policy`.
- **Biển số:** một xe có nhiều biển theo thời gian (1-N), biển cũ giữ làm lịch sử và **không bao giờ cấp lại**; `registration_expiry` nằm ở `vehicles`; `bookings.license_plate` lưu chữ biển lúc phát hành hợp đồng và lúc giao xe; đổi biển bằng `LicensePlateService.changePlate` (không sửa trực tiếp ô biển trong form xe); không đổi biển khi xe đang `rented`.
- **Không Docker**; PostgreSQL local; email Gmail SMTP tùy chọn; email khách trong seed dùng `@example.com`.
- **`schema.sql` xóa sạch rồi tạo lại**; không có migration.
- **Endpoint A1** được giữ tạm để team test thanh toán, sẽ xóa sau.
- **Không làm:** thanh toán thật, chữ ký số, React, thêm hệ thống phức tạp ngoài phạm vi đề tài.
