'use strict';
document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-screen="admin/vehicle-types/list"]');
    if (!root) return;
    root.addEventListener('change', event => {
        if (event.target.matches('[data-submit-on-change]')) event.target.form?.requestSubmit();
    });
    root.querySelectorAll('img[data-image-fallback]').forEach(image => {
        image.addEventListener('error', function () {
            image.src = image.dataset.imageFallback;
            image.removeAttribute('data-image-fallback');
        }, { once: true });
    });
});

