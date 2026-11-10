'use strict';
(() => {
        // Depot opening hours from crms.opening-hours.*: pickup and return both happen inside them.
        const hoursHost = document.getElementById('sch-grid');
        const OPEN = parseInt(hoursHost.dataset.openHour || '0', 10), CLOSE = parseInt(hoursHost.dataset.closeHour || '23', 10);
        const pad2 = (n) => String(n).padStart(2, '0');
        const ALL_DAY = OPEN <= 0 && CLOSE >= 23;
        const addDay = (dateStr) => {
            const d = new Date(dateStr + 'T00:00:00');
            d.setDate(d.getDate() + 1);
            return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
        };
        const weeks = Array.from(document.querySelectorAll('.sch-week'));
        const weekCount = weeks.reduce((max, w) => Math.max(max, Number(w.dataset.week) + 1), 0);
        const prev = document.getElementById('sch-prev');
        const next = document.getElementById('sch-next');
        const label = document.getElementById('sch-week-label');
        let week = 0;

        const showWeek = (w) => {
            week = Math.max(0, Math.min(weekCount - 1, w));
            const shown = weeks.filter((tb) => Number(tb.dataset.week) === week);
            weeks.forEach((tb) => { tb.hidden = Number(tb.dataset.week) !== week; });
            const first = shown[0].querySelector('.sch-day-date').textContent;
            const last = shown[shown.length - 1].querySelector('.sch-day-date').textContent;
            label.textContent = first.slice(0, 5) + ' – ' + last.slice(0, 5);
            prev.disabled = week === 0;
            next.disabled = week === weekCount - 1;
        };
        prev.addEventListener('click', () => showWeek(week - 1));
        next.addEventListener('click', () => showWeek(week + 1));
        if (weekCount) showWeek(0);

        const form = document.getElementById('sch-form');
        if (!form) return;
        const startDate = document.getElementById('sch-start-date');
        const startHour = document.getElementById('sch-start-hour');
        const endDate = document.getElementById('sch-end-date');
        const endHour = document.getElementById('sch-end-hour');
        const summary = document.getElementById('sch-summary');
        const message = document.getElementById('sch-message');
        const pad = (n) => String(n).padStart(2, '0');

        [startHour, endHour].forEach((select) => {
            for (let h = OPEN; h <= CLOSE; h++) select.add(new Option(pad(h) + ':00', h));
        });
        const cells = Array.from(document.querySelectorAll('.sch-cell'));
        cells.forEach((cell) => {
            const available = cell.dataset.state === 'free';
            cell.tabIndex = available ? 0 : -1;
            cell.setAttribute('role', available ? 'button' : 'gridcell');
            cell.setAttribute('aria-label', cell.title);
            if (available) cell.setAttribute('aria-pressed', 'false');
        });
        const firstDate = cells.length ? cells[0].dataset.date : '';
        const lastDate = cells.length ? cells[cells.length - 1].dataset.date : '';
        [startDate, endDate].forEach((input) => { input.min = firstDate; input.max = lastDate; });

        const toKey = (date, hour) => date + 'T' + pad(hour) + ':00';
        const cellByKey = new Map(cells.map((c) => [toKey(c.dataset.date, Number(c.dataset.hour)), c]));
        // 'YYYY-MM-DDTHH:00' moved by whole hours (local time, so no zone shifts)
        const shiftKey = (key, hours) => {
            const d = new Date(key + ':00');
            d.setHours(d.getHours() + hours);
            return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':00';
        };

        // Paint the chosen window and say what is wrong with it, if anything.
        const refresh = () => {
            cells.forEach((c) => {
                c.classList.remove('sch-selected', 'sch-buffer-preview');
                if (c.dataset.state === 'free') c.setAttribute('aria-pressed', 'false');
            });
            message.textContent = '';
            summary.textContent = '';
            if (!startDate.value || !endDate.value) return;
            const start = toKey(startDate.value, Number(startHour.value));
            const end = toKey(endDate.value, Number(endHour.value));
            if (end <= start) {
                message.textContent = 'Giờ trả xe phải sau giờ nhận xe.';
                return;
            }
            const blockedStates = new Set();
            cells.forEach((c) => {
                const key = toKey(c.dataset.date, Number(c.dataset.hour));
                if (key >= start && key < end) {
                    c.classList.add('sch-selected');
                    if (c.dataset.state === 'free') c.setAttribute('aria-pressed', 'true');
                    if (c.dataset.state !== 'free') blockedStates.add(c.dataset.state);
                }
            });
            // One buffer hour on each side. Before the pickup it may include the present moment; after the return it
            // must not touch another rental. Either must not run into a rental that is already booked.
            [shiftKey(start, -1), end].forEach((key) => {
                const cell = cellByKey.get(key);
                if (!cell) return;
                cell.classList.add('sch-buffer-preview');
                if (cell.dataset.state === 'booked') blockedStates.add('buffer');
            });
            const hours = Math.round((new Date(end) - new Date(start)) / 3600000);
            summary.textContent = startDate.value.split('-').reverse().join('/') + ' ' + pad(startHour.value) + ':00 → '
                + endDate.value.split('-').reverse().join('/') + ' ' + pad(endHour.value) + ':00 (' + hours + ' giờ)';
            if (blockedStates.has('past')) {
                message.textContent = 'Giờ nhận xe phải từ giờ kế tiếp trở đi.';
            } else if (blockedStates.has('buffer') && blockedStates.size === 1) {
                message.textContent = 'Cần chừa 1 giờ chuẩn bị xe trước và sau khung giờ thuê, chưa chạm vào đơn khác.';
            } else if (blockedStates.has('expired')) {
                message.textContent = 'Xe quá hạn đăng kiểm và chưa có lịch đăng kiểm trước ngày này. Hãy chọn ngày khác.';
            } else if (blockedStates.has('inspection')) {
                message.textContent = 'Xe đi đăng kiểm trong ngày này. Hãy chọn ngày khác.';
            } else if (blockedStates.size) {
                message.textContent = 'Khung giờ này trùng giờ xe đã có người thuê hoặc thời gian chuẩn bị xe. Hãy chọn khung khác.';
            }
        };

        // The end is the hour after the last chosen cell; for the day's last hour that is 00:00 of the next day.
        const setEnd = (dateStr, endAt) => {
            if (ALL_DAY && endAt === 24) {
                endDate.value = addDay(dateStr);
                endHour.value = 0;
            } else {
                endDate.value = dateStr;
                endHour.value = endAt;
            }
        };

        // First click picks the pickup hour, second click the last hour of the rental.
        let picking = 'start';
        cells.forEach((c) => {
          c.addEventListener('click', () => {
            if (c.dataset.state !== 'free') return;
            const hour = Number(c.dataset.hour);
            if (picking === 'start') {
                if (!ALL_DAY && (hour < OPEN || hour > CLOSE - 1)) {
                    message.textContent = 'Chỉ nhận xe từ ' + pad2(OPEN) + ':00 đến ' + pad2(CLOSE - 1) + ':00 để còn ít nhất 1 giờ thuê trước giờ đóng cửa.';
                    return;
                }
                startDate.value = c.dataset.date;
                startHour.value = hour;
                setEnd(c.dataset.date, hour + 1);
                picking = 'end';
            } else {
                const endAt = hour + 1;
                const start = toKey(startDate.value, Number(startHour.value));
                if (toKey(c.dataset.date, hour) < start) {
                    picking = 'start';
                    c.click();
                    return;
                }
                if (!ALL_DAY && (endAt < OPEN || endAt > CLOSE)) {
                    message.textContent = 'Chỉ trả xe từ ' + pad2(OPEN) + ':00 đến ' + pad2(CLOSE) + ':00.';
                    return;
                }
                setEnd(c.dataset.date, endAt);
                picking = 'start';
            }
            refresh();
          });
          c.addEventListener('keydown', (event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  c.click();
              }
          });
        });
        [startDate, startHour, endDate, endHour].forEach((el) => el.addEventListener('change', refresh));

        form.addEventListener('submit', (event) => {
            refresh();
            if (message.textContent) {
                event.preventDefault();
                return;
            }
            document.getElementById('sch-start').value = toKey(startDate.value, Number(startHour.value));
            document.getElementById('sch-end').value = toKey(endDate.value, Number(endHour.value));
        });
    })();
