# CRMS SaaS Admin UI System — Quy chuẩn Giao diện Dùng chung

> **Dành cho**: Kiệt, Phú, Phúc, Trí  
> **Xem mẫu trực tiếp tại**: `http://localhost:8081/admin/ui-kit`  
> **Phong cách thiết kế**: Hiện đại, tối giản theo chuẩn SaaS (Linear / Vercel), bo góc `rounded-lg`, viền mỏng 1px `#e2e8f0`, shadow nhẹ `shadow-xs`, typography Inter rõ ràng.

---

## 1. Nguyên tắc cốt lõi (Core Principles)
1. **Không dùng thẻ thô của Bootstrap cũ**: Tránh dùng các card viền đen, nút bóng to, hoặc badge sặc sỡ. Dùng các class `.admin-*` đã được định nghĩa sẵn trong `app.css`.
2. **Đồng bộ tiếng Việt**: Toàn bộ nhãn, tiêu đề, nút bấm, thông báo đều dùng tiếng Việt chuẩn, súc tích.
3. **Mô hình Component như React**: Chỉ cần gọi 1 dòng Thymeleaf fragment: `th:replace="~{fragments/admin-components :: componentName(...)}"`.

---

## 2. Thư viện Component & Code mẫu (Copy-Paste)

### A. Tiêu đề trang (`pageHeader`)
Dùng ở đầu mỗi trang quản trị. Hỗ trợ nút Quay lại (Back link).
```html
<div th:replace="~{fragments/admin-components :: pageHeader(
    title='Quản lý đơn thuê xe',
    subtitle='Theo dõi toàn bộ lịch trình, trạng thái giao nhận và thanh toán cọc',
    backUrl='/admin/dashboard',
    backText='Bảng điều khiển'
)}"></div>
```

---

### B. Thẻ thống kê chỉ số (`statCard`)
Dùng để hiển thị các số liệu KPI tổng quan trên đầu danh sách (hỗ trợ icon: `car`, `money`, `clock`, `check`; variant: `primary`, `success`, `warning`, `danger`).
```html
<div class="row g-3 mb-4">
    <div class="col-sm-6 col-lg-3">
        <div th:replace="~{fragments/admin-components :: statCard(
            label='TỔNG SỐ XE', 
            value='45', 
            subtext='Đang sẵn sàng cho thuê', 
            iconType='car', 
            variant='primary')}">
        </div>
    </div>
    <div class="col-sm-6 col-lg-3">
        <div th:replace="~{fragments/admin-components :: statCard(
            label='DOANH THU', 
            value='128.5M đ', 
            subtext='Tháng hiện tại', 
            iconType='money', 
            variant='success')}">
        </div>
    </div>
</div>
```

---

### C. Badge trạng thái (`statusBadge`)
Giúp các trạng thái trong toàn bộ dự án luôn có mã màu đồng nhất:
- `variant='success'`: Đã hoàn tất, Đang hoạt động, Đã duyệt (Xanh lá).
- `variant='info'`: Đã xác nhận cọc, Đang thuê (Xanh dương).
- `variant='warning'`: Chờ xử lý, Chờ hoàn cọc, Chờ duyệt (Vàng hổ phách).
- `variant='danger'`: Đã hủy, Bị từ chối, Gặp sự cố (Đỏ).
- `variant='neutral'`: Bản nháp, Chưa phân xe (Xám).

```html
<div th:replace="~{fragments/admin-components :: statusBadge(
    status='confirmed', 
    label='Đã xác nhận cọc', 
    variant='info')}">
</div>
```

---

### D. Thẻ Card chứa nội dung (`admin-card`)
Dùng để bọc bảng dữ liệu hoặc form nhập liệu:
```html
<div class="admin-card">
    <div class="admin-card-header">
        <div class="admin-card-title-group">
            <h2 class="admin-card-title">Danh sách khuyến mãi</h2>
            <p class="admin-card-subtitle">Quản lý mã coupon và chương trình ưu đãi</p>
        </div>
        <div class="admin-card-actions">
            <a th:href="@{/admin/promotions/new}" class="admin-btn admin-btn-primary admin-btn-sm">
                + Thêm mã mới
            </a>
        </div>
    </div>
    <div class="admin-card-body">
        <!-- Nội dung bảng hoặc form tại đây -->
    </div>
</div>
```

---

### E. Bảng dữ liệu SaaS (`admin-table`)
Bảng có hover dòng mượt, padding thoáng, text không bị xuống dòng vụn vặt:
```html
<div class="admin-table-wrapper">
    <table class="admin-table">
        <thead>
            <tr>
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Phương tiện</th>
                <th>Trạng thái</th>
                <th class="text-right">Thao tác</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="item : ${list}">
                <td class="font-mono font-weight-bold" th:text="'#' + ${item.id}">#101</td>
                <td th:text="${item.customerName}">Nguyễn Văn A</td>
                <td th:text="${item.vehicleName}">Toyota Vios</td>
                <td>
                    <div th:replace="~{fragments/admin-components :: statusBadge(
                        status=${item.status}, 
                        label=${item.statusLabel}, 
                        variant='success')}">
                    </div>
                </td>
                <td class="text-right">
                    <a th:href="@{/admin/items/{id}(id=${item.id})}" class="admin-btn admin-btn-secondary admin-btn-sm">
                        Xem chi tiết
                    </a>
                </td>
            </tr>
        </tbody>
    </table>
</div>
```

---

### F. Bộ nút bấm chuẩn (`admin-btn`)
```html
<!-- Nút chính (Đen navy) -->
<button type="submit" class="admin-btn admin-btn-primary">Lưu thay đổi</button>

<!-- Nút phụ viền xám (Trắng xám) -->
<a th:href="@{/admin/bookings}" class="admin-btn admin-btn-secondary">Hủy bỏ</a>

<!-- Nút thành công (Xanh lá) -->
<button type="button" class="admin-btn admin-btn-success">✓ Xác nhận bàn giao xe</button>

<!-- Nút nguy hiểm (Đỏ) -->
<button type="button" class="admin-btn admin-btn-danger">Hủy bỏ đơn</button>

<!-- Kích thước nhỏ (dùng trong bảng) -->
<button type="button" class="admin-btn admin-btn-secondary admin-btn-sm">Sửa</button>
```

---

### G. Form nhập liệu (`admin-form-*`)
```html
<div class="admin-form-group">
    <label class="admin-form-label">Mã khuyến mãi (*)</label>
    <input type="text" class="admin-form-input font-mono uppercase" placeholder="HE2026" required>
    <div class="admin-form-hint">Khách hàng sẽ nhập mã này ở bước thanh toán cọc.</div>
</div>

<div class="admin-form-group">
    <label class="admin-form-label">Loại giảm giá</label>
    <select class="admin-form-select">
        <option value="percent">Phần trăm (%)</option>
        <option value="fixed">Số tiền cố định (VND)</option>
    </select>
</div>

<div class="admin-form-group">
    <label class="admin-form-label">Ghi chú tình trạng xe</label>
    <textarea class="admin-form-textarea" rows="3" placeholder="Nhập tình trạng xe lúc nhận/trả..."></textarea>
</div>
```

---

### H. Hộp thoại Popup (Modal Dialog)
```html
<div id="myActionModal" class="admin-modal-backdrop hidden" onclick="if(event.target === this) this.classList.add('hidden')">
    <div class="admin-modal-card">
        <div class="admin-modal-header">
            <h4 class="admin-modal-title">Xác nhận bàn giao xe</h4>
            <button type="button" class="admin-modal-close" onclick="document.getElementById('myActionModal').classList.add('hidden')">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
            </button>
        </div>
        <div class="admin-modal-body text-sm text-slate-600">
            <!-- Nội dung form xác nhận tại đây -->
        </div>
        <div class="admin-modal-footer">
            <button type="button" class="admin-btn admin-btn-secondary" onclick="document.getElementById('myActionModal').classList.add('hidden')">Đóng</button>
            <button type="button" class="admin-btn admin-btn-primary">Xác nhận</button>
        </div>
    </div>
</div>
```

---

## 3. Hướng dẫn phân công chi tiết cho các bạn trong nhóm

### 1. Phú — Task: Nhận xe (Check-in) & Trả xe (Check-out)
- **Vị trí**: Gắn trực tiếp vào trang `manage.html` (`/admin/bookings/{id}`).
- **Check-in (Giao chìa khóa)**:
  - Kiểm tra đối chiếu: Checkbox `[✓] Đã đối chiếu bằng lái xe bản cứng (hạng B1/B2) và CCCD gốc của khách hàng`.
  - Nhập số ODO ban đầu (km) và mức nhiên liệu/pin bàn giao (ví dụ: 100%, 80%).
  - **Phân quyền**: Chỉ nhân viên phụ trách đơn (`session.employeeId == booking.employeeId`) hoặc Quản lý (`session.employeeRole == 'ADMIN'`) mới được bấm nút bàn giao.
- **Check-out (Nhận lại xe & Quyết toán)**:
  - Nhập ODO lúc trả, mức xăng/pin hoàn trả.
  - Phụ phí phát sinh: phí trễ giờ (đã tính tự động qua `pricingService`), phụ phí trầy xước/vệ sinh.
  - Hiển thị bảng tổng hợp quyết toán:
    $$\text{Phải thanh toán thêm} = \text{Tổng tiền thuê} + \text{Phí trễ} + \text{Phụ phí} - \text{Tiền đã cọc}$$

### 2. Trí — Task: Khuyến mãi (Promotions) & Đánh giá (Reviews)
- **Khuyến mãi**:
  - Tạo controller `AdminPromotionController` (`/admin/promotions`).
  - Dùng component `pageHeader`, `dataTable`, `statusBadge` để làm danh sách voucher.
  - Form tạo/sửa voucher: mã code, loại chiết khấu, giá trị, ngày hiệu lực, số lượng.
- **Đánh giá**:
  - Khách hàng sau khi hoàn tất chuyến (`completed`) được chấm sao (1-5 sao) và nhận xét.

### 3. Phúc — Task: Báo cáo sự cố (Issues & Incident Damage)
- **Vị trí**: Khi nhận lại xe lúc Check-out hoặc phát sinh trong hành trình.
- Form ghi nhận: mô tả sự cố (va chạm, hỏng lốp, xước sơn), mức phí đền bù (gắn với `BookingFee`), trạng thái xử lý (`open`, `investigating`, `resolved`).

