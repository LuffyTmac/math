(function () {
    'use strict';

    document.querySelectorAll('[data-stem]').forEach(function (el) {
        var content = el.getAttribute('data-stem') || '';
        el.textContent = '';
        var parts = content.split('□');
        parts.forEach(function (part, i) {
            if (part) {
                el.appendChild(document.createTextNode(part));
            }
            if (i < parts.length - 1) {
                var box = document.createElement('span');
                box.className = 'stem-blank';
                el.appendChild(box);
            }
        });
    });
})();
