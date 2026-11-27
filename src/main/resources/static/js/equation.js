(function () {
    'use strict';

    var body = document.body;
    var ctx = body.getAttribute('data-ctx') || '/';
    if (ctx.charAt(ctx.length - 1) !== '/') {
        ctx += '/';
    }

    var input = document.getElementById('eqInput');
    var keypad = document.getElementById('eqKeypad');
    var btnSubmit = document.getElementById('btnEqSubmit');
    var btnClear = document.getElementById('btnEqClear');
    var resultBox = document.getElementById('eqResult');
    var list = document.getElementById('eqList');
    var empty = document.getElementById('eqEmpty');
    var countEl = document.getElementById('eqCount');

    var MAX_LENGTH = 40;
    var HISTORY_LIMIT = 10;
    var submitting = false;

    function csrfToken() {
        var meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }

    function csrfHeader() {
        var meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    function append(ch) {
        if (input.value.length >= MAX_LENGTH) {
            return;
        }
        input.value += ch;
        hideResult();
    }

    function backspace() {
        input.value = input.value.slice(0, -1);
        hideResult();
    }

    function clearAll() {
        input.value = '';
        hideResult();
    }

    function hideResult() {
        resultBox.hidden = true;
    }

    function showResult(ok, text) {
        resultBox.hidden = false;
        resultBox.className = 'eq-result ' + (ok ? 'ok' : 'bad');
        resultBox.textContent = (ok ? '🎉 ' : '🤔 ') + text;
    }

    function addHistory(expression, correct) {
        if (!list) {
            return;
        }
        if (empty) {
            empty.style.display = 'none';
        }
        var li = document.createElement('li');
        li.className = 'eq-item ' + (correct ? 'ok' : 'bad');

        var mark = document.createElement('span');
        mark.className = 'eq-mark';
        mark.textContent = correct ? '✓' : '✗';

        var expr = document.createElement('span');
        expr.className = 'eq-expr';
        expr.textContent = expression;

        li.appendChild(mark);
        li.appendChild(expr);
        list.insertBefore(li, list.firstChild);
        while (list.children.length > HISTORY_LIMIT) {
            list.removeChild(list.lastChild);
        }
        if (countEl) {
            countEl.textContent = String(list.children.length);
        }
    }

    function submit() {
        if (submitting) {
            return;
        }
        // 提交前移除所有空白字符（前端兜底，后端也会再规范化一次）
        var expression = input.value.replace(/\s+/g, '');
        if (!expression) {
            showResult(false, '先写一个等式吧～');
            return;
        }

        submitting = true;
        btnSubmit.disabled = true;

        var headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        headers[csrfHeader()] = csrfToken();

        fetch(ctx + 'learn/equation/submit', {
            method: 'POST',
            credentials: 'same-origin',
            headers: headers,
            body: 'expression=' + encodeURIComponent(expression)
        }).then(function (resp) {
            if (!resp.ok) {
                throw new Error('http ' + resp.status);
            }
            return resp.json();
        }).then(function (data) {
            if (!data || !data.success) {
                showResult(false, (data && data.message) || '提交失败，再试一次吧～');
                return;
            }
            showResult(!!data.correct, data.message);
            addHistory(data.expression, !!data.correct);
            input.value = '';
        }).catch(function () {
            showResult(false, '提交没成功，检查一下网络再试试吧～');
        }).finally(function () {
            submitting = false;
            btnSubmit.disabled = false;
        });
    }

    if (keypad) {
        keypad.addEventListener('click', function (e) {
            var btn = e.target.closest ? e.target.closest('.eq-key') : null;
            if (!btn) {
                return;
            }
            var key = btn.getAttribute('data-key');
            if (key === 'back') {
                backspace();
            } else {
                append(key);
            }
        });
    }

    if (btnClear) {
        btnClear.addEventListener('click', clearAll);
    }
    if (btnSubmit) {
        btnSubmit.addEventListener('click', submit);
    }

    // 支持实体键盘（Pad 上也可以接键盘用）
    document.addEventListener('keydown', function (e) {
        var k = e.key;
        if (/^[0-9]$/.test(k) || k === '+' || k === '-' || k === '(' || k === ')' || k === '=') {
            append(k);
            e.preventDefault();
        } else if (k === '*' || k === 'x' || k === 'X' || k === '×') {
            append('×');
            e.preventDefault();
        } else if (k === 'Backspace') {
            backspace();
            e.preventDefault();
        } else if (k === 'Enter') {
            submit();
            e.preventDefault();
        }
    });
})();
