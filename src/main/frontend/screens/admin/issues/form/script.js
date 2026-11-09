'use strict';
document.addEventListener('DOMContentLoaded', function () {
    var input = document.getElementById('images');
    var note = document.getElementById('imagesCount');
    if (!input || !note) return;
    input.addEventListener('change', function () {
        var tooMany = input.files.length > 10;
        note.textContent = tooMany ? 'Chỉ được chọn tối đa 10 ảnh.' : '';
        if (tooMany) input.value = '';
    });
});
