'use strict';
document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-screen="admin/promotions/form"]');
    if (!root) return;
    const promotionDates = [
        { display: root.querySelector('#startDateDisplay'), value: root.querySelector('#startDate'), picker: root.querySelectorAll('.promotion-date-picker')[0] },
        { display: root.querySelector('#endDateDisplay'), value: root.querySelector('#endDate'), picker: root.querySelectorAll('.promotion-date-picker')[1] }
    ];
    const updatePromotionDate = date => {
        const match = date.display.value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
        if (!match) {
            date.value.value = '';
            date.display.setCustomValidity(date.display.value ? 'Nhập ngày theo định dạng dd/mm/yyyy.' : '');
            return;
        }
        const day = Number(match[1]);
        const month = Number(match[2]);
        const year = Number(match[3]);
        const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
        const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1] || 0;
        const valid = year > 0 && month >= 1 && month <= 12 && day >= 1 && day <= days;
        date.display.setCustomValidity(valid ? '' : 'Ngày không hợp lệ.');
        date.value.value = valid ? `${match[3]}-${match[2]}-${match[1]}` : '';
    };
    promotionDates.forEach(date => {
        if (!date.display || !date.value || !date.picker) return;
        date.display.addEventListener('input', () => updatePromotionDate(date));
        date.picker.addEventListener('change', () => {
            const [year, month, day] = date.picker.value.split('-');
            date.display.value = date.picker.value ? `${day}/${month}/${year}` : '';
            updatePromotionDate(date);
        });
        updatePromotionDate(date);
    });
});
