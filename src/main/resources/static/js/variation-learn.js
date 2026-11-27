(function () {
    'use strict';

    var body = document.body;
    var ctx = body.getAttribute('data-ctx') || '/';
    if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';

    function csrfToken() {
        var m = document.querySelector('meta[name="_csrf"]');
        return m ? m.getAttribute('content') : '';
    }
    function csrfHeader() {
        var m = document.querySelector('meta[name="_csrf_header"]');
        return m ? m.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    /* ==================== 居中提示 ==================== */
    var mask = document.getElementById('varMask');
    var modalEmoji = document.getElementById('varModalEmoji');
    var modalText = document.getElementById('varModalText');
    var modalOk = document.getElementById('varModalOk');
    var modalTimer = null;

    function showModal(emoji, text) {
        if (!mask) return;
        modalEmoji.textContent = emoji || '🎉';
        modalText.textContent = text;
        mask.classList.add('open');
        mask.setAttribute('aria-hidden', 'false');
        clearTimeout(modalTimer);
        modalTimer = setTimeout(hideModal, 2400);
    }
    function hideModal() {
        if (!mask) return;
        mask.classList.remove('open');
        mask.setAttribute('aria-hidden', 'true');
        clearTimeout(modalTimer);
    }
    if (modalOk) modalOk.addEventListener('click', hideModal);
    if (mask) {
        mask.addEventListener('click', function (e) {
            if (e.target === mask) hideModal();
        });
    }
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && mask && mask.classList.contains('open')) hideModal();
    });

    /* ==================== 答题键盘 ==================== */
    var kpMask = document.getElementById('kpMask');
    var kpDisplay = document.getElementById('kpDisplay');
    var kpGrid = document.getElementById('kpGrid');
    var kpClose = document.getElementById('kpClose');
    var activeInput = null;

    function openKeypad(input) {
        activeInput = input;
        if (kpDisplay) kpDisplay.textContent = input.value || '—';
        if (kpMask) {
            kpMask.classList.add('open');
            kpMask.setAttribute('aria-hidden', 'false');
        }
    }
    function closeKeypad() {
        if (kpMask) {
            kpMask.classList.remove('open');
            kpMask.setAttribute('aria-hidden', 'true');
        }
        activeInput = null;
    }
    if (kpClose) kpClose.addEventListener('click', closeKeypad);
    if (kpMask) {
        kpMask.addEventListener('click', function (e) {
            if (e.target === kpMask) closeKeypad();
        });
    }
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && kpMask && kpMask.classList.contains('open')) closeKeypad();
    });
    if (kpGrid) {
        kpGrid.addEventListener('click', function (e) {
            var btn = e.target.closest ? e.target.closest('button') : null;
            if (!btn || !activeInput) return;
            var key = btn.getAttribute('data-key');
            if (key === 'back') {
                activeInput.value = activeInput.value.slice(0, -1);
            } else if (key === 'clear') {
                activeInput.value = '';
            } else if (activeInput.value.length < 6) {
                activeInput.value += key;
            }
            if (kpDisplay) kpDisplay.textContent = activeInput.value || '—';
        });
    }

    /* ==================== 题干渲染 ==================== */
    var cards = document.querySelectorAll('.var-card');

    // 把题干里的 □ 渲染成输入框（含开头/结尾的空）
    cards.forEach(function (card) {
        var line = card.querySelector('.fill-line');
        if (!line) return;
        var content = line.getAttribute('data-content') || '';
        var parts = content.split('□');
        line.innerHTML = '';
        parts.forEach(function (part, i) {
            line.appendChild(document.createTextNode(part));
            if (i < parts.length - 1) {
                var input = document.createElement('input');
                input.type = 'text';
                input.className = 'blank';
                input.maxLength = 6;
                input.setAttribute('autocomplete', 'off');
                input.setAttribute('inputmode', 'none');
                input.readOnly = true;
                input.addEventListener('click', function () { openKeypad(input); });
                input.addEventListener('focus', function () { openKeypad(input); });
                line.appendChild(input);
            }
        });
    });

    // 选择题题干：□ 渲染成静态方框
    document.querySelectorAll('.choice-line').forEach(function (line) {
        var content = line.getAttribute('data-content') || '';
        var parts = content.split('□');
        line.innerHTML = '';
        parts.forEach(function (part, i) {
            line.appendChild(document.createTextNode(part));
            if (i < parts.length - 1) {
                var box = document.createElement('span');
                box.className = 'blank-static';
                line.appendChild(box);
            }
        });
    });

    /* ==================== 提交 ==================== */
    function isSubmitted(card) {
        return card.getAttribute('data-submitted') === 'true';
    }
    function markSubmitted(card, btn) {
        card.setAttribute('data-submitted', 'true');
        if (btn) {
            btn.textContent = '已提交答案';
            btn.classList.add('submitted');
        }
    }

    cards.forEach(function (card) {
        var qid = Number(card.getAttribute('data-qid'));
        var type = card.getAttribute('data-type');
        var submitBtn = card.querySelector('.var-submit');
        if (submitBtn && isSubmitted(card)) {
            markSubmitted(card, submitBtn);
        }

        if (type === 'JUDGE') {
            var buttons = card.querySelectorAll('.judge-btn');
            buttons.forEach(function (btn) {
                btn.addEventListener('click', function () {
                    buttons.forEach(function (b) { b.classList.remove('active'); });
                    btn.classList.add('active');
                });
            });
        }

        if (type === 'CHOICE') {
            var choiceButtons = card.querySelectorAll('.choice-btn');
            choiceButtons.forEach(function (btn) {
                btn.addEventListener('click', function () {
                    choiceButtons.forEach(function (b) { b.classList.remove('active'); });
                    btn.classList.add('active');
                });
            });
        }

        if (!submitBtn) return;

        submitBtn.addEventListener('click', function () {
            if (isSubmitted(card)) {
                showModal('😯', '你已经提交过这道题啦～');
                return;
            }
            if (submitBtn.disabled) return;

            var payload = { questionId: qid };

            if (type === 'FILL') {
                var inputs = card.querySelectorAll('.blank');
                var answers = [];
                var empty = false;
                inputs.forEach(function (input) {
                    var v = input.value.replace(/\s+/g, '');
                    if (!v) empty = true;
                    answers.push(v);
                });
                if (empty) {
                    showModal('😯', '把空都填完再提交哦～');
                    return;
                }
                payload.answers = answers;
            } else if (type === 'JUDGE') {
                var active = card.querySelector('.judge-btn.active');
                if (!active) {
                    showModal('😯', '先选「正确」或「错误」哦～');
                    return;
                }
                payload.judgeAnswer = active.getAttribute('data-answer');
            } else {
                var picked = card.querySelector('.choice-btn.active');
                if (!picked) {
                    showModal('😯', '先选一个答案哦～');
                    return;
                }
                payload.choiceAnswer = Number(picked.getAttribute('data-index'));
            }

            var oldLabel = submitBtn.textContent;
            submitBtn.disabled = true;
            submitBtn.classList.add('loading');
            submitBtn.textContent = '提交中…';

            var headers = { 'Content-Type': 'application/json;charset=UTF-8' };
            headers[csrfHeader()] = csrfToken();

            fetch(ctx + 'variation/submit', {
                method: 'POST',
                credentials: 'same-origin',
                headers: headers,
                body: JSON.stringify(payload)
            }).then(function (r) {
                return r.json();
            }).then(function (data) {
                if (data && data.success) {
                    markSubmitted(card, submitBtn);
                    showModal('🎉', data.message || '提交成功！');
                } else {
                    showModal('😯', (data && data.message) || '提交失败，再试一次吧～');
                }
            }).catch(function () {
                showModal('😯', '提交失败，检查网络后再试～');
            }).finally(function () {
                submitBtn.disabled = false;
                submitBtn.classList.remove('loading');
                // 已提交的题保持「已提交答案」，不要被恢复成「提交答案」
                submitBtn.textContent = isSubmitted(card) ? '已提交答案' : oldLabel;
            });
        });
    });
})();
