(function () {
    'use strict';

    var stage = document.getElementById('stage');
    var btn = document.getElementById('btnOrganize');
    var totalEl = document.getElementById('eqTotal');
    if (!stage || !btn) return;

    var ctx = document.body.getAttribute('data-ctx') || '/';
    if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';

    var reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var bubbles = [];
    var raf = null;
    var organized = false;
    var maxId = Number(stage.getAttribute('data-max-id') || 0);
    var pollTimer = null;

    function rand(min, max) {
        return min + Math.random() * (max - min);
    }
    function clamp(v, min, max) {
        return Math.min(max, Math.max(min, v));
    }

    function paint(b) {
        b.el.style.left = b.x.toFixed(2) + '%';
        b.el.style.top = b.y.toFixed(2) + '%';
    }

    function tick() {
        bubbles.forEach(function (b) {
            b.x += b.vx;
            b.y += b.vy;
            if (b.x < 6 || b.x > 94) {
                b.vx *= -1;
                b.x = clamp(b.x, 6, 94);
            }
            if (b.y < 9 || b.y > 91) {
                b.vy *= -1;
                b.y = clamp(b.y, 9, 91);
            }
            paint(b);
        });
        raf = requestAnimationFrame(tick);
    }

    function start() {
        if (reduced || raf) return;
        raf = requestAnimationFrame(tick);
    }
    function stop() {
        if (raf) {
            cancelAnimationFrame(raf);
            raf = null;
        }
    }

    function createBubbleEl(item) {
        var el = document.createElement('div');
        el.className = 'eq-bubble ' + (item.correct ? 'ok' : 'bad');
        var expr = document.createElement('span');
        expr.className = 'eq-b-expr';
        expr.textContent = item.expression;
        var who = document.createElement('span');
        who.className = 'eq-b-who';
        who.textContent = item.studentName || '学生';
        el.appendChild(expr);
        el.appendChild(who);
        return el;
    }

    function addBubble(el, isNew) {
        stage.appendChild(el);
        var b = {
            el: el,
            x: rand(8, 92),
            y: rand(12, 88),
            vx: rand(-0.06, 0.06),
            vy: rand(-0.05, 0.05)
        };
        paint(b);
        bubbles.push(b);

        if (isNew) {
            el.classList.add('is-new');
            setTimeout(function () {
                el.classList.remove('is-new');
            }, 1600);
            if (organized) {
                arrangeSlots();
            } else {
                start();
            }
        }
        return b;
    }

    function arrangeSlots() {
        if (!bubbles.length) return;
        stage.classList.add('arranging');
        var n = bubbles.length;
        var cols = Math.min(6, Math.max(3, Math.ceil(Math.sqrt(n * 1.6))));
        var rows = Math.ceil(n / cols);
        bubbles.forEach(function (b, i) {
            var c = i % cols;
            var r = Math.floor(i / cols);
            b.x = ((c + 0.5) / cols) * 100;
            b.y = ((r + 0.5) / rows) * 100;
            paint(b);
        });
        setTimeout(function () {
            stage.classList.remove('arranging');
        }, 600);
    }

    function organize() {
        organized = true;
        stop();
        arrangeSlots();
        btn.textContent = '🌪️ 重新飘散';
    }

    function scatter() {
        organized = false;
        stage.classList.add('arranging');
        bubbles.forEach(function (b) {
            b.x = rand(8, 92);
            b.y = rand(12, 88);
            b.vx = rand(-0.06, 0.06);
            b.vy = rand(-0.05, 0.05);
            paint(b);
        });
        btn.textContent = '🧩 整理';
        setTimeout(function () {
            stage.classList.remove('arranging');
            start();
        }, 600);
    }

    // 初始化已有气泡
    Array.prototype.forEach.call(stage.querySelectorAll('.eq-bubble'), function (el) {
        addBubble(el, false);
    });
    start();

    if (!bubbles.length) {
        btn.disabled = true;
    }

    btn.addEventListener('click', function () {
        if (organized) {
            scatter();
        } else {
            organize();
        }
    });

    // ---------- 自动拉取新提交 ----------
    function poll() {
        if (document.hidden) return;
        fetch(ctx + 'equation/admin/answers?sinceId=' + maxId, {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }).then(function (r) {
            return r.json();
        }).then(function (data) {
            if (!data || !data.success) return;
            maxId = data.maxId || maxId;
            if (totalEl) totalEl.textContent = data.total;

            var items = data.items || [];
            if (!items.length) return;

            items.forEach(function (item) {
                addBubble(createBubbleEl(item), true);
            });

            if (totalEl) totalEl.textContent = bubbles.length;
            if (organized) arrangeSlots();
        }).catch(function () {
            // 轮询失败忽略，下一轮再试
        });
    }

    pollTimer = setInterval(poll, 15000);

    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) poll();
    });

    window.addEventListener('beforeunload', function () {
        if (pollTimer) clearInterval(pollTimer);
    });
})();
