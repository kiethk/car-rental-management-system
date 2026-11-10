'use strict';
document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-screen="admin/promotions/form"]');
    if (!root) return;

    const setupDateField = (displayId, valueId) => {
        const display = root.querySelector(displayId);
        const value = root.querySelector(valueId);
        if (!display || !value) return;

        const updateIsoFromDisplay = () => {
            const match = display.value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
            if (!match) {
                value.value = '';
                display.setCustomValidity(display.value ? 'Nhập ngày theo định dạng dd/mm/yyyy.' : '');
                return;
            }
            const day = Number(match[1]);
            const month = Number(match[2]);
            const year = Number(match[3]);
            const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
            const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1] || 0;
            const valid = year > 0 && month >= 1 && month <= 12 && day >= 1 && day <= days;
            display.setCustomValidity(valid ? '' : 'Ngày không hợp lệ.');
            if (valid) {
                value.value = `${match[3]}-${match[2]}-${match[1]}`;
            } else {
                value.value = '';
            }
        };

        if (typeof window.flatpickr === 'function') {
            const locale = (window.flatpickr.l10ns && window.flatpickr.l10ns.vn) ? 'vn' : 'default';
            const fp = window.flatpickr(display, {
                locale,
                dateFormat: 'd/m/Y',
                allowInput: true,
                disableMobile: true,
                defaultDate: display.value || null,
                onChange: (selectedDates) => {
                    if (selectedDates && selectedDates.length > 0) {
                        const d = selectedDates[0];
                        const y = d.getFullYear();
                        const m = String(d.getMonth() + 1).padStart(2, '0');
                        const day = String(d.getDate()).padStart(2, '0');
                        display.value = `${day}/${m}/${y}`;
                        value.value = `${y}-${m}-${day}`;
                        display.setCustomValidity('');
                    } else {
                        value.value = '';
                    }
                }
            });

            display.addEventListener('input', () => {
                updateIsoFromDisplay();
                if (value.value) {
                    fp.setDate(value.value, false, 'Y-m-d');
                }
            });
            display.addEventListener('change', () => {
                updateIsoFromDisplay();
                if (value.value) {
                    fp.setDate(value.value, false, 'Y-m-d');
                }
            });
        } else {
            display.addEventListener('input', updateIsoFromDisplay);
            display.addEventListener('change', updateIsoFromDisplay);
        }

        updateIsoFromDisplay();
    };

    setupDateField('#startDateDisplay', '#startDate');
    setupDateField('#endDateDisplay', '#endDate');
});
