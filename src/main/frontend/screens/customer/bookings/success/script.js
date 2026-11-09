'use strict';

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-fallback-src]').forEach((image) => {
        image.addEventListener('error', () => {
            image.src = image.dataset.fallbackSrc;
            image.removeAttribute('data-fallback-src');
        }, { once: true });
    });
});

