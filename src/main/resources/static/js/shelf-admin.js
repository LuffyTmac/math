(function () {
    'use strict';

    var mask = document.getElementById('shelfModal');
    var body = document.getElementById('shelfModalBody');
    var nameEl = document.getElementById('shelfModalName');
    var configEl = document.getElementById('shelfModalConfig');
    var closeBtn = document.getElementById('shelfModalClose');
    var items = document.querySelectorAll('.shelf-item');

    function openModal(item) {
        if (!mask || !body) return;
        var shelf = item.querySelector('.ms-shelf');
        if (!shelf) return;

        body.innerHTML = '';
        body.appendChild(shelf.cloneNode(true));

        nameEl.textContent = item.getAttribute('data-student') || '学生';
        configEl.textContent = item.getAttribute('data-config') || '';

        mask.classList.add('open');
        mask.setAttribute('aria-hidden', 'false');
    }

    function closeModal() {
        if (!mask) return;
        mask.classList.remove('open');
        mask.setAttribute('aria-hidden', 'true');
    }

    items.forEach(function (item) {
        item.addEventListener('click', function () {
            openModal(item);
        });
    });

    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (mask) {
        mask.addEventListener('click', function (e) {
            if (e.target === mask) closeModal();
        });
    }
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && mask && mask.classList.contains('open')) closeModal();
    });
})();
