'use strict';
document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-screen="admin/vehicle-types/form"]');
    if (!root) return;
    const fileInput = root.querySelector('#imageFileInput');
    const fileName = root.querySelector('#chosenFileName');
    const clearButton = root.querySelector('#clearFileBtn');
    const urlInput = root.querySelector('#imageUrlInput');
    const help = root.querySelector('#imageUrlHelp');
    if (!fileInput || !fileName || !clearButton || !urlInput || !help) return;
    root.querySelectorAll('img[data-image-fallback]').forEach(image => {
        image.addEventListener('error', function () {
            image.src = image.dataset.imageFallback;
            image.removeAttribute('data-image-fallback');
        }, { once: true });
    });

    const syncUpload = () => {
        const file = fileInput.files && fileInput.files[0];
        fileName.textContent = file ? file.name : 'Chưa chọn tệp';
        fileName.classList.toggle('text-muted', !file);
        fileName.classList.toggle('fw-bold', Boolean(file));
        clearButton.hidden = !file;
        urlInput.readOnly = Boolean(file);
        urlInput.classList.toggle('is-upload-locked', Boolean(file));
        help.textContent = file
            ? 'Ô đường dẫn ảnh đã bị khóa vì bạn đã chọn tệp từ máy.'
            : 'Dán đường dẫn ảnh trên mạng nếu không tải tệp.';
        help.classList.toggle('text-warning', Boolean(file));
    };

    fileInput.addEventListener('change', syncUpload);
    root.querySelector('[data-clear-vehicle-type-image]')?.addEventListener('click', function () {
        fileInput.value = '';
        syncUpload();
    });
    syncUpload();
});
