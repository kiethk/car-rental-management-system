/*
 * Confirmation dialog that replaces the browser's confirm().
 *
 * Put data-confirm on a <form> (asked on submit), on a submit <button> (asked when it submits its form)
 * or on a link (asked on click):
 *
 *   data-confirm="Xóa xe này?"            the question (required)
 *   data-confirm-detail="..."             one short line under it (optional)
 *   data-confirm-ok="Xóa"                 label of the confirm button (default "Xác nhận")
 *   data-confirm-cancel="Quay lại"        label of the dismiss button (default "Hủy")
 *   data-confirm-tone="danger"            red confirm button for destructive actions
 */
(function () {
    'use strict';

    var dialog = null;

    function build() {
        var overlay = document.createElement('div');
        overlay.className = 'crms-confirm';
        overlay.style.display = 'none';
        overlay.innerHTML =
            '<div class="crms-confirm__panel" role="alertdialog" aria-modal="true"' +
            ' aria-labelledby="crms-confirm-title" aria-describedby="crms-confirm-detail">' +
            '<h2 class="crms-confirm__title" id="crms-confirm-title"></h2>' +
            '<p class="crms-confirm__detail" id="crms-confirm-detail"></p>' +
            '<div class="crms-confirm__actions">' +
            '<button type="button" class="crms-confirm__btn crms-confirm__btn--ghost" data-role="cancel"></button>' +
            '<button type="button" class="crms-confirm__btn" data-role="ok"></button>' +
            '</div></div>';
        document.body.appendChild(overlay);
        return {
            overlay: overlay,
            title: overlay.querySelector('.crms-confirm__title'),
            detail: overlay.querySelector('.crms-confirm__detail'),
            cancel: overlay.querySelector('[data-role="cancel"]'),
            ok: overlay.querySelector('[data-role="ok"]')
        };
    }

    function ask(source, onConfirm) {
        if (dialog && dialog.overlay.classList.contains('is-open')) {
            return;
        }
        dialog = dialog || build();
        var danger = source.getAttribute('data-confirm-tone') === 'danger';
        var detail = source.getAttribute('data-confirm-detail');
        var previouslyFocused = document.activeElement;

        dialog.title.textContent = source.getAttribute('data-confirm');
        dialog.detail.textContent = detail || '';
        dialog.detail.hidden = !detail;
        dialog.cancel.textContent = source.getAttribute('data-confirm-cancel') || 'Hủy';
        dialog.ok.textContent = source.getAttribute('data-confirm-ok') || 'Xác nhận';
        dialog.ok.className = 'crms-confirm__btn ' + (danger ? 'crms-confirm__btn--danger' : 'crms-confirm__btn--primary');

        function close() {
            dialog.overlay.classList.remove('is-open');
            document.removeEventListener('keydown', onKey, true);
            dialog.overlay.removeEventListener('mousedown', onBackdrop);
            dialog.cancel.removeEventListener('click', onCancel);
            dialog.ok.removeEventListener('click', onOk);
            window.setTimeout(function () {
                if (!dialog.overlay.classList.contains('is-open')) {
                    dialog.overlay.style.display = 'none';
                }
            }, 160);
            if (previouslyFocused && previouslyFocused.focus) {
                previouslyFocused.focus();
            }
        }

        function onKey(event) {
            if (event.key === 'Escape') {
                event.preventDefault();
                close();
            } else if (event.key === 'Tab') {
                // keep focus inside the two buttons
                event.preventDefault();
                (document.activeElement === dialog.ok ? dialog.cancel : dialog.ok).focus();
            }
        }

        function onBackdrop(event) {
            if (event.target === dialog.overlay) {
                close();
            }
        }

        function onCancel() {
            close();
        }

        function onOk() {
            close();
            onConfirm();
        }

        dialog.cancel.addEventListener('click', onCancel);
        dialog.ok.addEventListener('click', onOk);
        document.addEventListener('keydown', onKey, true);
        dialog.overlay.addEventListener('mousedown', onBackdrop);

        dialog.overlay.style.display = 'flex';
        // next frame, so the opacity transition runs
        window.requestAnimationFrame(function () {
            dialog.overlay.classList.add('is-open');
            // destructive actions start on the safe choice
            (danger ? dialog.cancel : dialog.ok).focus();
        });
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form || form.tagName !== 'FORM' || form.__crmsConfirmed) {
            return;
        }
        var submitter = event.submitter;
        var source = form.hasAttribute('data-confirm') ? form
            : (submitter && submitter.hasAttribute('data-confirm') ? submitter : null);
        if (!source) {
            return;
        }
        event.preventDefault();
        ask(source, function () {
            form.__crmsConfirmed = true;
            try {
                if (form.requestSubmit) {
                    form.requestSubmit(submitter || undefined);
                } else {
                    form.submit();
                }
            } finally {
                form.__crmsConfirmed = false;
            }
        });
    }, true);

    document.addEventListener('click', function (event) {
        var link = event.target.closest ? event.target.closest('a[data-confirm]') : null;
        if (!link || event.defaultPrevented) {
            return;
        }
        event.preventDefault();
        ask(link, function () {
            window.location.href = link.href;
        });
    }, true);
})();
