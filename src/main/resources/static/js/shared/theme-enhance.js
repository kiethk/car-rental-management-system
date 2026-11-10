'use strict';

/*
 * Progressive UI enhancement for every screen. It only adds decorative nodes and classes,
 * and never changes existing ids, names, values or data-* hooks.
 */
(function () {
    const BUTTON_ICONS = [
        [/^(\+\s*)?(thêm|tạo|new|add)/i, 'plus'],
        [/sửa|chỉnh|cập nhật|edit/i, 'pencil'],
        [/xóa|xoá|delete|remove/i, 'trash-2'],
        [/lưu|save/i, 'save'],
        [/tìm|lọc|search|filter/i, 'search'],
        [/xuất|tải xuống|export|download/i, 'download'],
        [/tải lên|upload/i, 'upload'],
        [/hủy|huỷ|đóng|cancel|close/i, 'x'],
        [/duyệt|xác nhận|approve|confirm|hoàn tất/i, 'check'],
        [/từ chối|reject/i, 'ban'],
        [/xem|chi tiết|view|detail/i, 'eye'],
        [/quay lại|trở về|back/i, 'arrow-left'],
        [/in |print/i, 'printer'],
        [/đặt xe|thuê|book/i, 'car-front'],
        [/đăng nhập|login/i, 'log-in'],
        [/đăng xuất|logout/i, 'log-out'],
        [/thanh toán|pay/i, 'credit-card'],
        [/kiểm định|kiểm tra|inspect/i, 'clipboard-check'],
    ];

    const HEADER_ICONS = [
        [/biển số|plate/i, 'rectangle-horizontal'],
        [/khách|người|họ tên|tài khoản|user|tên/i, 'user'],
        [/xe|dòng xe|vehicle/i, 'car'],
        [/ngày|thời gian|date|lịch/i, 'calendar'],
        [/trạng thái|status/i, 'activity'],
        [/giá|tiền|tổng|phí|amount|price/i, 'wallet'],
        [/mã|id|#/i, 'hash'],
        [/email/i, 'mail'],
        [/điện thoại|sđt|phone/i, 'phone'],
        [/ghi chú|mô tả|note/i, 'sticky-note'],
    ];

    const PERSON_HEADER = /khách hàng|họ tên|người|tài khoản|customer|user/i;
    const PLATE_RE = /^\s*\d{2}[A-Z]{1,2}\d?[-\s]?\d{3}\.?\d{2}\s*$/;

    const iconFor = (text, table) => {
        for (const [re, name] of table) if (re.test(text)) return name;
        return null;
    };

    const makeIcon = (name) => {
        const i = document.createElement('i');
        i.setAttribute('data-lucide', name);
        i.setAttribute('aria-hidden', 'true');
        return i;
    };

    function decorateButtons(root) {
        root.querySelectorAll('.btn, .admin-btn').forEach((btn) => {
            if (btn.dataset.evIcon || btn.querySelector('svg, i, img')) return;
            const label = btn.textContent.trim();
            if (!label || label.length > 40 || /^[+←→✓×]/.test(label)) return;
            const name = iconFor(label, BUTTON_ICONS);
            if (!name) return;
            btn.dataset.evIcon = name;
            btn.prepend(makeIcon(name));
        });
    }

    function decorateTables(root) {
        root.querySelectorAll('table.table').forEach((table) => {
            const headers = Array.from(table.querySelectorAll('thead th'));
            const personCols = new Set();
            headers.forEach((th, idx) => {
                const text = th.textContent.trim();
                if (!text || th.querySelector('svg, i, input')) return;
                const name = iconFor(text, HEADER_ICONS);
                if (name) th.prepend(makeIcon(name));
                if (PERSON_HEADER.test(text)) personCols.add(idx);
            });

            table.querySelectorAll('tbody tr').forEach((tr) => {
                Array.from(tr.children).forEach((td, idx) => {
                    if (td.dataset.evDone) return;
                    td.dataset.evDone = '1';

                    if (td.children.length === 0 && PLATE_RE.test(td.textContent)) {
                        const plate = document.createElement('span');
                        plate.className = 'ev-plate';
                        plate.textContent = td.textContent.trim();
                        td.textContent = '';
                        td.append(plate);
                        return;
                    }
                    td.querySelectorAll('.font-mono').forEach((el) => {
                        if (PLATE_RE.test(el.textContent) && !el.closest('.admin-plate-badge')) el.classList.add('ev-plate');
                    });

                    if (personCols.has(idx) && !td.querySelector('img, .ev-avatar, input, select')) {
                        const nameEl = td.querySelector('strong, .fw-bold, .fw-semibold') || td;
                        const name = (nameEl.firstChild && nameEl.firstChild.nodeType === 3 ? nameEl.firstChild.textContent : nameEl.textContent).trim();
                        if (!name || /^[\d#—-]/.test(name) || name.length > 60) return;
                        const parts = name.split(/\s+/).filter(Boolean);
                        const initials = (parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].slice(0, 2)).toUpperCase();
                        const wrap = document.createElement('div');
                        wrap.className = 'ev-person';
                        const avatar = document.createElement('span');
                        avatar.className = 'ev-avatar';
                        avatar.setAttribute('aria-hidden', 'true');
                        avatar.textContent = initials;
                        const body = document.createElement('div');
                        while (td.firstChild) body.append(td.firstChild);
                        wrap.append(avatar, body);
                        td.append(wrap);
                    }
                });
            });
        });
    }

    function initDatePickers(root) {
        if (typeof window.flatpickr !== 'function') return;
        const locale = window.flatpickr.l10ns && window.flatpickr.l10ns.vn ? 'vn' : 'default';
        root.querySelectorAll('input[type="date"], input[type="datetime-local"]').forEach((input) => {
            if (input._flatpickr || input.hasAttribute('data-rental-role') || input.hasAttribute('data-native-date')) return;
            const withTime = input.type === 'datetime-local';
            const altFormat = input.dataset.altFormat || (withTime ? 'd/m/Y H:i' : 'd/m/Y');
            window.flatpickr(input, {
                locale,
                enableTime: withTime,
                time_24hr: true,
                dateFormat: withTime ? 'Y-m-d\\TH:i' : 'Y-m-d',
                altInput: true,
                altFormat,
                altInputClass: input.className,
                minDate: input.min || null,
                maxDate: input.max || null,
                defaultDate: input.value ? input.value.slice(0, withTime ? 16 : 10) : null,
                disableMobile: true,
                onChange: () => input.dispatchEvent(new Event('input', { bubbles: true })),
            });
        });
    }

    function initUtilizationChart() {
        const el = document.getElementById('evUtilizationChart');
        if (!el || typeof window.ApexCharts !== 'function') return;
        const n = (k) => Number(el.dataset[k]) || 0;
        const series = [n('available'), n('rented'), n('maintenance')];
        const chart = new window.ApexCharts(el, {
            chart: { type: 'donut', height: 240, fontFamily: 'Roboto, sans-serif', animations: { easing: 'easeinout', speed: 700 } },
            series,
            labels: ['Sẵn sàng', 'Đang thuê', 'Bảo dưỡng'],
            colors: ['#16A34A', '#F59E0B', '#DC2626'],
            legend: { show: false },
            dataLabels: { enabled: false },
            stroke: { width: 3, colors: ['#FFFFFF'] },
            plotOptions: {
                pie: {
                    donut: {
                        size: '74%',
                        labels: {
                            show: true,
                            value: { fontFamily: 'Roboto Mono, monospace', fontWeight: 800, fontSize: '28px', color: '#0A192F' },
                            total: { show: true, label: 'Tổng xe', color: '#64748B', fontSize: '12px', formatter: () => String(series.reduce((a, b) => a + b, 0)) },
                        },
                    },
                },
            },
            tooltip: { theme: 'light' },
        });
        chart.render();
    }

    function run() {
        decorateButtons(document);
        decorateTables(document);
        initDatePickers(document);
        initUtilizationChart();
        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons({ attrs: { 'stroke-width': 2 } });
        }
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', run);
    else run();
})();
