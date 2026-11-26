/**
 * 乘法分配律 · 学生端（Pad）交互
 *
 *  1. 页面只有卡片，点卡片打开答题层（大图 + 符号键盘）
 *  2. 只允许两种写法：a*(b+c) 与 a*b+a*c（含减号），与服务端 ExpressionValidator 一致
 *  3. 成功 / 格式错误 / 提交失败都用**页面正中的提示框**告诉孩子
 */
(function () {
    'use strict';

    var body = document.body;
    var ctx = body.getAttribute('data-ctx') || '/';
    if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';

    var mask = document.getElementById('sheetMask');
    var input = document.getElementById('exprInput');
    var keypad = document.getElementById('keypad');
    var btnSubmit = document.getElementById('btnSubmit');
    var btnCancel = document.getElementById('btnCancel');
    var btnClose = document.getElementById('sheetClose');
    var sheetImg = document.getElementById('sheetImg');

    var msgMask = document.getElementById('msgMask');
    var msgEmoji = document.getElementById('msgEmoji');
    var msgText = document.getElementById('msgText');
    var msgOk = document.getElementById('msgOk');
    var msgAlt = document.getElementById('msgAlt');

    if (!mask || !input) return;

    var currentCard = null;
    var submitting = false;
    var msgTimer = null;
    var lastActiveAt = Date.now();
    var LONG_IDLE_MS = 20 * 60 * 1000; // 20 分钟没有任何操作，视为“可能过期”

    /* ==================== 校验（与服务端同一套规则） ==================== */

    function normalize(raw) {
        var s = raw == null ? '' : String(raw);
        if (s.normalize) {
            try { s = s.normalize('NFKC'); } catch (e) { /* 老浏览器忽略 */ }
        }
        s = s.replace(/[×✕✖xX∗]/g, '*')
             .replace(/[−﹣–—]/g, '-')
             .replace(/＋/g, '+')
             .replace(/（/g, '(')
             .replace(/）/g, ')');
        return s.replace(/[\s\u00A0\u3000]+/g, '');
    }

    function bracketsBalanced(s) {
        var depth = 0;
        for (var i = 0; i < s.length; i++) {
            if (s.charAt(i) === '(') depth++;
            else if (s.charAt(i) === ')') { depth--; if (depth < 0) return false; }
        }
        return depth === 0;
    }

    function validate(raw) {
        var s = normalize(raw);
        if (!s) return { ok: false, msg: '先写一个算式吧～' };
        if (s.length > 200) return { ok: false, msg: '算式太长啦，简单一点哦～' };
        if (!/^[0-9+\-*()]+$/.test(s)) return { ok: false, msg: '只能用数字和 + - × ( ) 哦～' };
        if (s.indexOf('*') < 0) return { ok: false, msg: '分配律里要出现乘号 × 哦～' };
        if (!bracketsBalanced(s)) return { ok: false, msg: '括号要成对出现，像这样：(4+5)' };

        if (/^\d+\*\(\d+[+-]\d+\)$/.test(s)) return { ok: true, expr: s };

        var m = /^(\d+)\*(\d+)([+-])(\d+)\*(\d+)$/.exec(s);
        if (m) {
            if (Number(m[1]) !== Number(m[4]) && Number(m[2]) !== Number(m[5])) {
                return { ok: false, msg: '两个乘法要有一个相同的数，比如 3×4＋3×5 里的 3 哦～' };
            }
            return { ok: true, expr: s };
        }

        var inner = /\(([^()]*)\)/.exec(s);
        if (inner && !/^\d+[+-]\d+$/.test(inner[1])) {
            return { ok: false, msg: '括号里写成「4+5」这样就好啦～' };
        }
        return { ok: false, msg: '试试写成 3×(4+5) 或 3×4＋3×5 吧～' };
    }

    /* ==================== 居中提示框 ==================== */

    function reloadPage() {
        window.location.reload();
    }

    function showMsg(type, text, opts) {
        opts = opts || {};
        msgMask.setAttribute('data-type', type);
        msgEmoji.textContent = opts.emoji
            || (type === 'error' ? '😯' : (type === 'success' ? '🎉' : '⏰'));
        msgText.textContent = text;

        msgOk.textContent = opts.okLabel || '好的';
        msgOk.onclick = function () {
            hideMsg();
            if (opts.onOk) opts.onOk();
        };

        if (opts.altLabel) {
            msgAlt.style.display = '';
            msgAlt.textContent = opts.altLabel;
            msgAlt.onclick = function () {
                hideMsg();
                if (opts.onAlt) opts.onAlt();
            };
        } else {
            msgAlt.style.display = 'none';
            msgAlt.onclick = null;
        }

        msgMask.classList.add('open');
        msgMask.setAttribute('aria-hidden', 'false');

        clearTimeout(msgTimer);
        if (opts.autoClose) {
            msgTimer = setTimeout(hideMsg, opts.autoClose);
        }
    }

    function hideMsg() {
        clearTimeout(msgTimer);
        msgMask.classList.remove('open');
        msgMask.setAttribute('aria-hidden', 'true');
    }

    if (msgMask) {
        msgMask.addEventListener('click', function (e) {
            if (e.target === msgMask) hideMsg();
        });
    }

    /* ==================== 符号键盘 ==================== */

    var KEYS = [
        ['7', 'num'], ['8', 'num'], ['9', 'num'], ['×', 'op'],
        ['4', 'num'], ['5', 'num'], ['6', 'num'], ['(', 'op'],
        ['1', 'num'], ['2', 'num'], ['3', 'num'], [')', 'op'],
        ['0', 'num'], ['+', 'op'], ['-', 'op'], ['⌫', 'util'],
        ['清空', 'util clear']
    ];

    function buildKeypad() {
        KEYS.forEach(function (item) {
            var key = document.createElement('button');
            key.type = 'button';
            key.className = 'key ' + item[1];
            key.textContent = item[0];
            key.addEventListener('click', function () {
                if (item[0] === '⌫') input.value = input.value.slice(0, -1);
                else if (item[0] === '清空') input.value = '';
                else input.value += item[0];
            });
            keypad.appendChild(key);
        });
    }

    /* ==================== 答题层 ==================== */

    function openSheet(card) {
        currentCard = card;
        var img = card.getAttribute('data-img') || '';

        sheetImg.src = img;
        sheetImg.style.visibility = img ? 'visible' : 'hidden';

        input.value = '';
        mask.classList.add('open');
        mask.setAttribute('aria-hidden', 'false');
        document.body.classList.add('sheet-open');
        setTimeout(function () { input.focus(); }, 60);
    }

    function closeSheet() {
        mask.classList.remove('open');
        mask.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('sheet-open');
        currentCard = null;
    }

    /* ==================== 提交 ==================== */

    function csrfToken() {
        var meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }
    function csrfHeader() {
        var meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    /** 页面是否“放太久没动过” */
    function isStalePage() {
        return Date.now() - lastActiveAt > LONG_IDLE_MS;
    }

    /** 提交入口：先看页面是否过旧，过旧先友好提示，可选择刷新或直接提交 */
    function submit() {
        if (submitting) return;
        var check = validate(input.value);
        if (!check.ok) {
            showMsg('error', check.msg);
            return;
        }
        if (!currentCard) {
            showMsg('error', '题目信息丢失，刷新页面再试试吧～', {
                okLabel: '🔄 刷新页面', onOk: reloadPage
            });
            return;
        }
        if (isStalePage()) {
            showMsg('warn', '页面放了好一会儿啦～ 刷新一下再提交，交上去更稳妥哦。', {
                emoji: '⏰',
                okLabel: '🔄 刷新页面', onOk: reloadPage,
                altLabel: '直接提交', onAlt: doSubmit
            });
            return;
        }
        doSubmit();
    }

    /** 真正发请求 */
    function doSubmit() {
        if (submitting) return;
        var check = validate(input.value);
        if (!check.ok) {
            showMsg('error', check.msg);
            return;
        }
        if (!currentCard) return;

        submitting = true;
        btnSubmit.disabled = true;
        var oldText = btnSubmit.textContent;
        btnSubmit.textContent = '提交中…';

        var headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        headers[csrfHeader()] = csrfToken();

        fetch(ctx + 'distributive/submit', {
            method: 'POST',
            credentials: 'same-origin',
            headers: headers,
            body: 'questionId=' + encodeURIComponent(currentCard.getAttribute('data-qid'))
                + '&expression=' + encodeURIComponent(check.expr)
        }).then(function (resp) {
            if (resp.status === 403 || resp.status === 401) {
                return { success: false, stale: true, message: '页面太久没刷新啦，刷新一下再提交吧～' };
            }
            if (!resp.ok) {
                return { success: false, stale: true, message: '提交没成功，稍后再试一次吧～' };
            }
            return resp.json();
        }).then(function (data) {
            if (data && data.success) {
                lastActiveAt = Date.now();
                var card = currentCard;
                closeSheet();
                card.classList.add('done');
                var go = card.querySelector('.qgo');
                if (go) go.textContent = '继续提交';
                showMsg('success', data.message || '太棒了！已经交给老师啦～', { autoClose: 2600 });
                return;
            }

            var message = (data && data.message) || '提交失败，再试一次吧～';
            if (data && data.stale) {
                showMsg('error', message, {
                    okLabel: '🔄 刷新页面', onOk: reloadPage,
                    altLabel: '再试一次', onAlt: doSubmit
                });
                return;
            }
            // 题目已被删除/下架等“页面数据过期”的情况，建议刷新
            if (message.indexOf('不存在') >= 0 || message.indexOf('下架') >= 0) {
                showMsg('error', message, { okLabel: '🔄 刷新页面', onOk: reloadPage });
                return;
            }
            showMsg('error', message);
        }).catch(function () {
            showMsg('error', '提交没成功，可能是网络或页面太久没刷新了～', {
                okLabel: '🔄 刷新页面', onOk: reloadPage,
                altLabel: '再试一次', onAlt: doSubmit
            });
        }).finally(function () {
            submitting = false;
            btnSubmit.disabled = false;
            btnSubmit.textContent = oldText;
        });
    }
    // 记录最近一次操作时间，用于判断页面是否“放太久”
    ['pointerdown', 'keydown', 'touchstart', 'wheel'].forEach(function (evt) {
        document.addEventListener(evt, function () { lastActiveAt = Date.now(); }, { passive: true });
    });

    /* ==================== 事件绑定 ==================== */

    document.querySelectorAll('.qcard').forEach(function (card) {
        card.addEventListener('click', function () { openSheet(card); });
        card.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                openSheet(card);
            }
        });
    });

    btnClose.addEventListener('click', closeSheet);
    btnCancel.addEventListener('click', closeSheet);
    btnSubmit.addEventListener('click', submit);
    mask.addEventListener('click', function (e) {
        if (e.target === mask) closeSheet();
    });

    document.addEventListener('keydown', function (e) {
        if (msgMask.classList.contains('open')) {
            if (e.key === 'Escape' || e.key === 'Enter') { e.preventDefault(); hideMsg(); }
            return;
        }
        if (!mask.classList.contains('open')) return;
        if (e.key === 'Escape') { closeSheet(); return; }
        if (e.key === 'Enter') { e.preventDefault(); submit(); return; }
        if (e.key === 'Backspace') {
            e.preventDefault();
            input.value = input.value.slice(0, -1);
            return;
        }
        if (/^[0-9+\-*()]$/.test(e.key)) {
            e.preventDefault();
            input.value += e.key;
        }
    });

    buildKeypad();
})();
