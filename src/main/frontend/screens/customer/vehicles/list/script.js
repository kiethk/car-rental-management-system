'use strict';

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-fallback-src]').forEach((image) => {
        image.addEventListener('error', () => {
            image.src = image.dataset.fallbackSrc;
            image.removeAttribute('data-fallback-src');
        }, { once: true });
    });
});


document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-action="submit-filter"]').forEach((control) => {
        control.addEventListener('change', () => document.getElementById('filterForm')?.requestSubmit());
    });
});
