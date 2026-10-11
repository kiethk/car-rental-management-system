# Kiểm tra hiện trạng

Nguồn local đã đọc ngày 11/10/2026, múi giờ Việt Nam:

- UI: `D:\FA26_FPT\HSF302_LamNN15\car-rental-ui`
- ERD: `D:\FA26_FPT\HSF302_LamNN15\Diagrams\erd_car.drawio`
- Project PE: `D:\FA26_FPT\HSF302_LamNN15\Projects\SE2033_CE200178_AMG_PE`

## UI

React 19, TypeScript, Vite 8, Tailwind 4. Package và config gốc còn tên/plugin Figma Make.

- `src/main.tsx` mount `Root.tsx`.
- `Root.tsx` tách customer/admin; admin dùng hash, lazy loading.
- `App.tsx` chuyển màn khách hàng bằng React state; refresh quay về home.
- `components/`: header, icon, chặn theo trạng thái GPLX ở giao diện.
- `data/mockData.ts`: type và dữ liệu mẫu.
- `data/rentalStore.ts`: store dùng chung customer/admin trong bộ nhớ; refresh mất cập nhật.
- `data/rentalLogic.ts`: quy tắc gán xe, vòng đời booking, thanh toán, hủy/hoàn cọc.

13 màn khách hàng:

Home, Search, Vehicle Detail, Booking Flow, Checkout, Payment, Booking Success, My Bookings, Booking Detail, Login, Register, Profile, Driving License.

Dashboard + 10 mục admin:

Bookings, Driving License Verification, Vehicle Types, Vehicles, License Plates, Vehicle Inspections, Issues, Payments, Promotions, Reviews.

Luồng mẫu: `pending_payment → confirmed → active → completed`; hủy từ `pending_payment/confirmed`, trước giờ nhận xe. Gán xe sau xác nhận, ưu tiên xe khách chọn, kiểm tra trùng lịch. Cọc, thanh toán cuối, hoàn tiền tách thành transaction mẫu.

Auth đang so sánh password trong dữ liệu giả; đăng ký chỉ tạo object trong bộ nhớ; admin chưa được bảo vệ bởi backend. Các mục admin ngoài booking/xe/payment phần lớn dùng local state riêng. Không thấy kết nối API/DB qua fetch hoặc axios trong `src`.

UI test có sẵn: 17/17 pass. TypeScript `--noEmit`: pass. Chưa đánh giá giao diện trực quan trên trình duyệt; nhận xét cấu trúc dựa trên source.

Repo UI có remote `https://github.com/kiethk/car-rental-management-system-ui.git`. Lệnh trạng thái Git tại source trả lỗi work tree trong môi trường hiện tại; chưa xác nhận cây source sạch hay đã commit đầy đủ. Không sửa source hoặc Git source.

## ERD — kết quả đúng

Đếm **đối tượng đang hiển thị**, không dùng `mermaidData`/`mermaidId` để đếm:

- `conceptual_erd`: **13 thực thể, 17 quan hệ**.
- `physical_erd`: **8 bảng**, phiên bản cũ hơn conceptual.
- Mermaid nhúng vẫn chỉ có các thực thể cũ; nhiều đối tượng copy còn trùng `mermaidId`. Label/source/target hiện tại mới phản ánh conceptual đã chỉnh.

13 thực thể: CUSTOMER, EMPLOYEE, VEHICLE_TYPE, VEHICLE, LICENSE_PLATE, SLOT, BOOKING, LICENSE_DOCUMENT, PAYMENT, PROMOTION, REVIEW, VEHICLE_INSPECTION, ISSUE.

5 thực thể thiếu trong physical: VEHICLE_TYPE, LICENSE_PLATE, SLOT, VEHICLE_INSPECTION, ISSUE.

Người dùng đã xác nhận: **SLOT = khung giờ cho thuê của từng loại xe**.

## Project PE

Spring Boot 4.1.1, Java 21, Maven; Web MVC, Thymeleaf, JPA, Validation, Bootstrap; MSSQL.

Packages: controller, dto, entity, repository, service, service/impl. `HomeController → ArtifactService → ArtifactServiceImpl → ArtifactRepository → Artifact`. Template dùng Model và `th:each`.

Đây là mức cấu trúc nhóm muốn dùng. Base giữ cấu trúc đó, dùng PostgreSQL theo lựa chọn database trước đây. Không sao chép các hàm PE còn `return false`, mật khẩu local PE, hoặc test package cũ không trùng application package.
