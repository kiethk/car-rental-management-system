'use strict';

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-fallback-src]').forEach((image) => {
        image.addEventListener('error', () => {
            image.src = image.dataset.fallbackSrc;
            image.removeAttribute('data-fallback-src');
        }, { once: true });
    });

    const timer = document.getElementById('depositTimer');
    const button = document.getElementById('submitPaymentBtn');
    if (!timer || !button) return;

    const expiry = Date.parse(timer.dataset.expiresAt || '');
    const badge = timer.closest('.holding-badge');
    const expire = () => {
        timer.textContent = '00:00 (Hết hạn)';
        badge?.classList.add('is-expired');
        button.disabled = true;
        button.setAttribute('aria-disabled', 'true');
        button.textContent = 'Đơn đặt xe đã hết hạn thanh toán';
    };
    if (!Number.isFinite(expiry)) {
        expire();
        return;
    }

    const update = () => {
        const seconds = Math.max(0, Math.ceil((expiry - Date.now()) / 1000));
        timer.textContent = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
        if (seconds === 0) {
            expire();
            return false;
        }
        return true;
    };
    if (update()) {
        const interval = window.setInterval(() => {
            if (!update()) window.clearInterval(interval);
        }, 1000);
    }
});
