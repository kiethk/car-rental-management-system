'use strict';
document.addEventListener('DOMContentLoaded', function () {
    const root = document.querySelector('[data-screen="admin/inspections/result"]');
    if (!root) return;
    const passed = root.querySelector('#passedSection');
    const failed = root.querySelector('#failedNotice');
    const update = value => {
        if (passed) passed.hidden = value !== 'passed';
        if (failed) failed.hidden = value !== 'failed';
    };
    root.querySelectorAll('[data-inspection-result]').forEach(input => {
        input.addEventListener('change', () => update(input.dataset.inspectionResult));
    });
    const selected = root.querySelector('[data-inspection-result]:checked');
    if (selected) update(selected.dataset.inspectionResult);
});
