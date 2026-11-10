'use strict';
document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-screen="admin/vehicles/list"]');
    if (!root) return;
    root.addEventListener('change', event => {
        if (event.target.matches('[data-submit-on-change]')) event.target.form?.requestSubmit();
    });
});

