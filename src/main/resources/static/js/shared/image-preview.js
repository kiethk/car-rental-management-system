'use strict';

/*
 * Shared image preview: click/keyboard opens a lightbox, and image file inputs
 * get a small clickable preview thumbnail. Only decorative behavior; it never
 * changes form fields, names or existing data-* hooks.
 */
(() => {
    let overlay, overlayImage, closeButton, lastFocus;

    const buildOverlay = () => {
        if (overlay) return;
        overlay = document.createElement('div');
        overlay.className = 'image-lightbox';
        overlay.setAttribute('role', 'dialog');
        overlay.setAttribute('aria-modal', 'true');
        overlay.setAttribute('aria-label', 'Xem ảnh phóng to');

        closeButton = document.createElement('button');
        closeButton.type = 'button';
        closeButton.className = 'image-lightbox-close';
        closeButton.setAttribute('aria-label', 'Đóng ảnh');
        closeButton.textContent = '×';

        overlayImage = document.createElement('img');
        overlayImage.alt = '';

        overlay.append(overlayImage, closeButton);
        document.body.appendChild(overlay);

        const close = () => {
            overlay.classList.remove('is-open');
            document.body.classList.remove('image-preview-open');
            if (lastFocus && typeof lastFocus.focus === 'function') lastFocus.focus();
            lastFocus = null;
        };
        closeButton.addEventListener('click', close);
        overlay.addEventListener('click', (event) => {
            if (event.target === overlay) close();
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && overlay.classList.contains('is-open')) close();
        });
    };

    const open = (src, alt, trigger) => {
        if (!src) return;
        buildOverlay();
        lastFocus = trigger || document.activeElement;
        overlayImage.src = src;
        overlayImage.alt = alt || '';
        overlay.classList.add('is-open');
        document.body.classList.add('image-preview-open');
        closeButton.focus();
    };

    const bindZoom = (element) => {
        if (!element || element.dataset.imageZoomBound === '1') return;
        const image = element.tagName === 'IMG' ? element : element.querySelector('img');
        if (!image) return;
        element.dataset.imageZoomBound = '1';
        element.classList.add('image-zoom-trigger');
        if (!element.hasAttribute('tabindex')) element.setAttribute('tabindex', '0');
        element.setAttribute('role', 'button');
        element.setAttribute('aria-label', 'Phóng to ảnh' + (image.alt ? ': ' + image.alt : ''));
        const activate = (event) => {
            event.preventDefault();
            open(image.currentSrc || image.src, image.alt, element);
        };
        element.addEventListener('click', activate);
        element.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                activate(event);
            }
        });
    };

    const bindFileInputs = () => {
        document.querySelectorAll('input[type="file"][accept*="image"]').forEach((input) => {
            if (input.dataset.imagePreviewBound === '1') return;
            input.dataset.imagePreviewBound = '1';

            const preview = document.createElement('figure');
            preview.className = 'image-upload-preview';
            preview.hidden = true;

            const image = document.createElement('img');
            image.alt = 'Ảnh xem trước';
            image.loading = 'lazy';
            image.decoding = 'async';
            image.setAttribute('data-image-zoom', '');

            const caption = document.createElement('figcaption');
            caption.textContent = 'Ảnh xem trước — bấm để phóng to';
            preview.append(image, caption);

            const anchor = input.closest('.form-group, .admin-form-group, .form-group-custom, .col-12, .col-md-6')
                || input.parentElement;
            anchor.appendChild(preview);

            let objectUrl = null;
            input.addEventListener('change', () => {
                const file = input.files && input.files[0];
                if (objectUrl) {
                    URL.revokeObjectURL(objectUrl);
                    objectUrl = null;
                }
                if (!file || !file.type.startsWith('image/')) {
                    preview.hidden = true;
                    image.removeAttribute('src');
                    return;
                }
                objectUrl = URL.createObjectURL(file);
                image.src = objectUrl;
                image.alt = 'Ảnh xem trước: ' + file.name;
                preview.hidden = false;
                bindZoom(image);
            });
        });
    };

    const init = () => {
        document.querySelectorAll('img[data-image-zoom], a[data-image-zoom]').forEach(bindZoom);
        bindFileInputs();
    };

    window.ImagePreview = { refresh: init };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true });
    else init();
})();
