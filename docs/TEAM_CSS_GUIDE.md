# QUY CHUẨN VIẾT CSS & ĐẶT TÊN CLASS — CRMS TEAM

> **Dành cho tất cả thành viên (Kiệt, Phú, Phúc, Trí)**  
> Nhằm tránh xung đột class (CSS collision), vỡ layout khi gộp nhánh (merge code), và giữ cho `app.css` gọn gàng, dễ bảo trì.

---

## 1. Kiến Trúc CSS Theo Module (Thymeleaf Modular CSS)

Từ nay dự án áp dụng kiến trúc **CSS Modular**:

```
src/main/resources/static/css/
├── app.css                   # CHỈ CHỨA: Design tokens (:root), Navbar, Footer, Utility chung
├── drivenow.css              # Reset & Tailwind core dựng sẵn
└── modules/                  # THƯ MỤC CHỨA CSS RIÊNG CỦA TỪNG TÍNH NĂNG
    ├── booking.css           # Luồng đặt xe (Kiệt: checkout, deposit, success, detail)
    ├── vehicles.css          # Quản lý đội xe, danh mục xe (Phúc)
    ├── inspections.css       # Kiểm định, giao nhận xe (Phúc)
    ├── licenses.css          # Duyệt GPLX, quản lý tài khoản (Phú)
    ├── promotions.css        # Khuyến mãi, mã giảm giá (Trí)
    └── issues.css            # Sự cố, phạt, bồi thường (Trí)
```

### 3 Nguyên Tắc Cốt Lõi:
1. **KHÔNG nhồi nhét CSS của tính năng riêng vào `app.css`**: `app.css` chỉ chứa biến màu `:root`, style của Header/Footer và các class dùng chung toàn bộ hệ thống.
2. **KHÔNG viết inline `<style>` trong file HTML**: Khối `<style>` trong HTML làm file phình to hàng trăm dòng, không được trình duyệt cache và rất khó debug xung đột.
3. **Tách CSS riêng của màn hình vào thư mục `static/css/modules/`**: Mỗi màn hình hoặc luồng chức năng load module tương ứng qua thẻ `<link>` trong `<head>`:
   ```html
   <!-- Luôn đặt sau app.css để ghi đè style nếu cần -->
   <link rel="stylesheet" th:href="@{/css/app.css}">
   <link rel="stylesheet" th:href="@{/css/modules/booking.css}">
   ```

---

## 2. Quy Ước Đặt Tên Class Chống Trùng (Namespacing)

Để đảm bảo bạn viết class ở màn hình của bạn **không bao giờ đè lên hoặc làm hỏng** giao diện của bạn khác, bắt buộc thêm **Tiền tố (Prefix)** theo module cho mọi custom class.

### Bảng tiền tố phân chia theo thành viên:

| Tiền tố (Prefix) | Phạm vi sử dụng | Phụ trách | Ví dụ Class hợp lệ |
| :--- | :--- | :--- | :--- |
| `crms-*` | **Chỉ dùng chung toàn hệ thống** (đặt trong `app.css`) | Cả nhóm | `.crms-container`, `.crms-status-pill`, `.crms-dropdown` |
| `booking-*`, `deposit-*`, `checkout-*` | Luồng đặt xe, thanh toán cọc, chi tiết đơn | **Kiệt** | `.booking-card`, `.deposit-timer-badge`, `.checkout-summary-box` |
| `veh-*`, `fleet-*` | Danh mục xe, chi tiết xe, quản lý xe | **Phúc** | `.veh-card-img`, `.fleet-filter-bar`, `.veh-plate-tag` |
| `insp-*` | Biên bản kiểm tra, bàn giao xe, ODO | **Phúc** | `.insp-checklist-card`, `.insp-photo-grid`, `.insp-odometer-input` |
| `lic-*` | Xác minh GPLX, upload tài liệu CCCD | **Phú** | `.lic-upload-dropzone`, `.lic-status-alert`, `.lic-preview-modal` |
| `promo-*` | Danh sách khuyến mãi, nhập voucher | **Trí** | `.promo-ticket-box`, `.promo-code-input`, `.promo-discount-tag` |
| `issue-*` | Ghi nhận sự cố, biên bản phạt | **Trí** | `.issue-report-table`, `.issue-severity-badge`, `.issue-fee-row` |
| `admin-*` | Các thành phần bảng/nút dùng trong trang Admin | Nhóm Admin | `.admin-table`, `.admin-btn`, `.admin-status-badge` |

---

### Quy tắc "Nên & Không nên" khi đặt tên:

| ❌ TUYỆT ĐỐI KHÔNG DÙNG (Dễ đụng hàng) | ✅ NÊN DÙNG (Có Prefix & Ý nghĩa rõ ràng) |
| :--- | :--- |
| `.card` | `.booking-surface-card` hoặc `.veh-detail-card` |
| `.title`, `.heading` | `.deposit-page-title` hoặc `.promo-heading` |
| `.container` | `.crms-container` (chuẩn 1280px) |
| `.btn-custom`, `.button` | `.admin-btn-primary`, `.booking-pay-btn` |
| `.table` | `.admin-table`, `.insp-history-table` |
| `.badge`, `.status` | `.crms-status-pill`, `.lic-verified-badge` |
| `.left`, `.right`, `.col` | `.booking-left-col`, `.checkout-summary-col` |
| `.info`, `.desc` | `.veh-spec-info`, `.deposit-instruction-text` |

> **Nguyên tắc vàng BEM thu gọn**: `[tiền-tố]-[thành-phần]-[chi-tiết]`  
> *Ví dụ*: `.booking-schedule-item`, `.insp-photo-thumbnail`, `.promo-voucher-input`.

---

## 3. Quy Chuẩn Kích Thước Khung Hình (Layout & Width)

Tất cả các trang trong hệ thống (cả trang Khách hàng lẫn Admin) phải tuân theo chuẩn độ rộng để thẳng hàng với Header (Logo bên trái, Menu/Avatar bên phải):

1. **Độ rộng khung chứa chuẩn (Max Container Width)**:
   - Sử dụng `.crms-container` hoặc class Tailwind `max-w-7xl` (**`1280px`**).
   - Padding ngang chuẩn: `px-4 sm:px-6` (hoặc `padding: 0 1.5rem`).
   - Căn giữa: `margin: 0 auto`.

   ```html
   <!-- Chuẩn khung trang khách hàng -->
   <main class="crms-container py-6">
       <!-- Nội dung màn hình -->
   </main>
   ```

2. **Tỷ lệ Grid 2 Cột trên Desktop (`min-width: 992px`)**:
   - Khi chia màn hình 2 cột (ví dụ: Thông tin bên trái, Tóm tắt/Thanh toán bên phải):
     ```css
     .yourmodule-grid-layout {
         display: grid;
         grid-template-columns: 1fr;
         gap: 24px;
     }
     @media (min-width: 992px) {
         .yourmodule-grid-layout {
             grid-template-columns: 1.5fr 1fr; /* hoặc 2fr 1fr */
             gap: 32px;
             align-items: start;
         }
     }
     ```

---

## 4. Bảng Màu & Typography Chuẩn (DriveNow Tokens)

Không tự ý phối các màu lạ. Hãy sử dụng hệ màu có sẵn trong `:root` của `app.css`:

```css
:root {
  --color-navy: #1e293b;        /* Màu chủ đạo (Text, Header, Button chính) */
  --color-navy-dark: #0f172a;   /* Nền banner, text tiêu đề đậm */
  --color-amber: #ea580c;       /* Màu nhấn nổi bật (Nút thanh toán, icon lưu ý) */
  --color-surface: #f8fafc;     /* Nền xám nhạt toàn trang (bg-surface) */
  --color-border: #e2e8f0;      /* Đường viền card, bảng (#e2e8f0) */
  --color-muted: #64748b;       /* Chữ phụ, subtitle, ngày giờ */
}
```

* **Font chữ**: Sử dụng `'Inter', sans-serif` cho văn bản thường.
* **Font số liệu / Biển số / Mã đơn**: Sử dụng `'JetBrains Mono', monospace` (`font-mono`) cho biển số xe, số tiền VNĐ, mã giao dịch.

---

## 5. Checklist Trước Khi Commit & Push Code

Mỗi khi thành viên viết hoặc chỉnh sửa giao diện, vui lòng tự kiểm tra:
- [ ] Không có khối `<style>` nội bộ nào nằm trực tiếp trong file HTML (đã tách ra file `.css` trong `static/css/modules/`).
- [ ] Mọi class tự định nghĩa đều có tiền tố tương ứng (`admin-*`, `veh-*`, `insp-*`, `lic-*`, `promo-*`, `booking-*`).
- [ ] Không sửa/xóa các class trong `app.css` khi chưa thống nhất với người viết ban đầu.
- [ ] Độ rộng trang không bị vượt quá `1280px` (`max-w-7xl` / `.crms-container`).
- [ ] Chạy `mvn test` (hoặc `.\mvnw.cmd test-compile`) thành công trước khi tạo PR.

