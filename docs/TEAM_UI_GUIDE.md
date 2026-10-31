# UI LAYOUT GUIDE — CRMS Team

All members (**Kiet, Phu, Phuc, Tri**) must follow this DriveNow Design System when building Thymeleaf views to ensure visual consistency across the entire application.

> 💡 **QUY CHUẨN CSS**: Vui lòng đọc chi tiết [Quy chuẩn viết CSS & Đặt tên class chống trùng lặp (TEAM_CSS_GUIDE.md)](TEAM_CSS_GUIDE.md) để biết cách chia file `modules/*.css` và đặt tiền tố (prefix) tránh xung đột code.

---

## 1. Brand Design Tokens

- **Font**: `Inter` (Google Fonts: 400, 500, 600, 700)
- **Color palette**:
  - `Navy Primary`: `#202a35` — classes: `bg-navy`, `text-navy`, `btn-navy`
  - `Amber Accent`: `#b45309` — classes: `bg-amber`, `text-amber`, `btn-amber`
  - `App Background`: `#f6f7f8` — class: `bg-surface`
  - `Card / Box`: class `card-drivenow` (white bg, light gray border, subtle hover shadow)

---

## 2. Shared Layout Fragments

| Component | File Path | Usage Snippet |
|---|---|---|
| **Header (Navbar)** | `templates/fragments/header.html` | `<div th:replace="~{fragments/header :: header}"></div>` |
| **Footer** | `templates/fragments/footer.html` | `<div th:replace="~{fragments/footer :: footer}"></div>` |
| **Admin Sidebar** | `templates/fragments/admin-sidebar.html` | `<div th:replace="~{fragments/admin-sidebar :: sidebar(active='tab_name')}"></div>` |

---

## 3. Template: Customer-Facing Pages

*Use for: Browse Cars, Vehicle Detail, Booking Form, My Bookings, My License*

```html
<!doctype html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Page Title — DriveNow</title>
    <!-- Required CSS for every page -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" th:href="@{/css/bootstrap.css}">
    <link rel="stylesheet" th:href="@{/css/drivenow.css}">
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body class="d-flex flex-column min-vh-100 bg-surface">

    <!-- 1. SHARED HEADER -->
    <div th:replace="~{fragments/header :: header}"></div>

    <!-- 2. MAIN CONTENT -->
    <main class="container my-4 flex-grow-1">
        <div class="card card-drivenow border-0 p-4">
            <h2 class="h4 font-weight-bold text-navy mb-3">Page Heading</h2>
            <!-- Your table / form content here -->
        </div>
    </main>

    <!-- 3. SHARED FOOTER -->
    <div th:replace="~{fragments/footer :: footer}"></div>

</body>
</html>
```

---

## 4. Template: Admin / Staff Pages

*Use for: `/admin/vehicles`, `/admin/bookings`, `/admin/licenses`, `/admin/promotions`, `/admin/issues`*

Two-column layout: fixed left sidebar + right content area.

```html
<!doctype html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Admin — DriveNow</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" th:href="@{/css/bootstrap.css}">
    <link rel="stylesheet" th:href="@{/css/drivenow.css}">
    <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body class="d-flex flex-column min-vh-100 bg-surface">

    <!-- 1. SHARED HEADER -->
    <div th:replace="~{fragments/header :: header}"></div>

    <!-- 2. ADMIN TWO-COLUMN LAYOUT -->
    <main class="container-fluid px-lg-5 my-4 flex-grow-1">
        <div class="row">

            <!-- LEFT COLUMN: SIDEBAR -->
            <div class="col-md-3 col-lg-2">
                <!-- Replace 'vehicles' with your module's active key (see table below) -->
                <div th:replace="~{fragments/admin-sidebar :: sidebar(active='vehicles')}"></div>
            </div>

            <!-- RIGHT COLUMN: YOUR CONTENT -->
            <div class="col-md-9 col-lg-10">
                <div class="card card-drivenow border-0 p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h1 class="h4 font-weight-bold text-navy mb-0">Fleet Management</h1>
                        <a href="#" class="btn btn-navy btn-sm">+ Add Vehicle</a>
                    </div>
                    <!-- Your data table / form here -->
                </div>
            </div>

        </div>
    </main>

    <!-- 3. SHARED FOOTER -->
    <div th:replace="~{fragments/footer :: footer}"></div>

</body>
</html>
```

---

## 5. Sidebar `active` Reference Table

| Member | Module | URL Route | `active` value |
|---|---|---|---|
| **Kiet** | Dashboard | `/admin/dashboard` | `'dashboard'` |
| **Kiet** | Rental Lifecycle (Booking) | `/admin/bookings` | `'bookings'` |
| **Phuc** | Fleet & License Plates | `/admin/vehicles` | `'vehicles'` |
| **Phuc** | Inspection & Maintenance | `/admin/inspections` | `'inspections'` |
| **Tri** | Promotions & Vouchers | `/admin/promotions` | `'promotions'` |
| **Tri** | Incidents & Fines | `/admin/issues` | `'issues'` |
| **Phu** | Driving License Review | `/admin/licenses` | `'licenses'` |
| **Phu** | Account Management | `/admin/accounts` | `'accounts'` |

---

## 6. UI Utility Classes Quick Reference

**Buttons:**
- `btn btn-navy btn-sm` — primary navy brand button
- `btn btn-amber btn-sm` — amber accent button (use for pay / book actions)
- `btn btn-outline-secondary btn-sm` — secondary / cancel button

**Status Badges:**
```html
<span class="badge badge-success px-2 py-1">Available / Approved</span>
<span class="badge badge-warning px-2 py-1">Rented / Pending</span>
<span class="badge badge-danger px-2 py-1">Cancelled / Rejected</span>
<span class="badge badge-secondary px-2 py-1">Maintenance</span>
```

**Currency format (VND):**
```html
<span th:text="${#numbers.formatDecimal(price, 0, 'COMMA', 0, 'POINT')}">150,000</span> VND
```
