'use strict';
document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-screen="admin/vehicle-types/form"]');
    if (!root) return;
    const fileInput = root.querySelector('#imageFileInput');
    const fileName = root.querySelector('#chosenFileName');
    const clearButton = root.querySelector('#clearFileBtn');
    root.querySelectorAll('img[data-image-fallback]').forEach(image => {
        image.addEventListener('error', function () {
            image.src = image.dataset.imageFallback;
            image.removeAttribute('data-image-fallback');
        }, { once: true });
    });
    if (!fileInput || !fileName || !clearButton) return;

    const syncUpload = () => {
        const file = fileInput.files && fileInput.files[0];
        fileName.textContent = file ? file.name : 'Chưa chọn tệp';
        fileName.classList.toggle('text-muted', !file);
        fileName.classList.toggle('fw-bold', Boolean(file));
        clearButton.hidden = !file;
    };

    fileInput.addEventListener('change', syncUpload);
    root.querySelector('[data-clear-vehicle-type-image]')?.addEventListener('click', function () {
        fileInput.value = '';
        // Let the shared image preview (and syncUpload) react to the cleared file
        fileInput.dispatchEvent(new Event('change', { bubbles: true }));
    });
    syncUpload();
});
