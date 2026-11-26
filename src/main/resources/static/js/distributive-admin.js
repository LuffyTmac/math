/**
 * 乘法分配律 · 教师端「科幻云海 + 甬道归类」
 *
 *  1. 默认只有一行图片卡片贴在底部，甬道收起，整块上方都留给算式飘动
 *  2. 点「开始归类」：甬道展开、卡片被顶到顶部，算式一个一个飞到各自甬道，落地成 chip
 *  3. 「再次归类」把 chip 收回去，甬道重新收起，回到云海漂浮状态
 *  4. 每 15 秒可自动拉取新提交的算式；归类状态下有新算式时自动回到云海
 *
 * DOM 约定（admin_list.html）：
 *   #stage #field #row .col[data-qid] .qcard .tunnel .lane[data-lane] .tunnel-head .num
 *   #btnMagic #cntTotal #doneFlag #exprData .expr-item[data-id][data-qid][data-time]
 *   .dist-content[data-max-id]
 */
(function () {
    'use strict';

    var stage = document.getElementById('stage');
    if (!stage) return;

    var body = document.body;
    var content = document.querySelector('.dist-content');
    var field = document.getElementById('field');
    var row = document.getElementById('row');
    var btn = document.getElementById('btnMagic');
    var cntTotal = document.getElementById('cntTotal');
    var fieldEmpty = document.getElementById('fieldEmpty');
    var pollState = document.getElementById('pollState');
    var btnRefresh = document.getElementById('btnRefresh');
    var btnClear = document.getElementById('btnClear');
    var clearMask = document.getElementById('clearMask');
    var clearConfirm = document.getElementById('clearConfirm');
    var clearCancel = document.getElementById('clearCancel');
    var doneFlag = document.getElementById('doneFlag');
    var exprData = document.getElementById('exprData');

    var reducedMotion = window.matchMedia
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    var PALETTE = ['', 'c2', 'c3', 'c4'];
    var POLL_INTERVAL = 15000;
    var TUNNEL_TRANSITION = reducedMotion ? 0 : 470;

    /* ==================== 状态 ==================== */

    var answers = [];                 // {id, qid, expression, time, no}
    var bubbles = [];                 // {a, el, x, y, w, h, vx, vy}
    var byQid = {};
    var busy = false;
    var drifting = false;
    var rafId = null;
    var pollTimer = null;
    var pendingItems = [];

    (function readInitialAnswers() {
        var order = {};
        row.querySelectorAll('.col').forEach(function (col, index) {
            var qid = col.getAttribute('data-qid');
            order[qid] = index + 1;
            byQid[qid] = col;
        });
        exprData.querySelectorAll('.expr-item').forEach(function (item) {
            answers.push({
                id: parseInt(item.getAttribute('data-id'), 10) || 0,
                qid: item.getAttribute('data-qid'),
                expression: (item.textContent || '').trim(),
                time: item.getAttribute('data-time') || '',
                no: order[item.getAttribute('data-qid')] || ''
            });
        });
    })();

    /* ==================== 基础工具 ==================== */

    function clamp(v, min, max) {
        return v < min ? min : (v > max ? max : v);
    }
    function rand(a, b) {
        return a + Math.random() * (b - a);
    }
    function fieldSize() {
        var rect = field.getBoundingClientRect();
        return { w: rect.width, h: rect.height };
    }
    function cardHeight() {
        var card = row.querySelector('.qcard');
        return card ? card.offsetHeight : 180;
    }
    function isOrganized() {
        return row.classList.contains('organized');
    }
    function updateDensity() {
        var n = answers.length;
        field.classList.toggle('density-compact', n > 30 && n <= 80);
        field.classList.toggle('density-dense', n > 80);
    }

    /**
     * 云海下边界 = 卡片高度 + 间距。
     * 未归类时卡片贴底，云海一直延伸到卡片上沿；
     * 归类后云海隐藏，不需要再算。
     */
    function layoutField() {
        if (isOrganized()) return;
        field.style.bottom = (cardHeight() + 24) + 'px';
    }

    /* ==================== 甬道开合 ==================== */

    function tunnelHeight() {
        var h = row.clientHeight - cardHeight() - 9;
        return (!isFinite(h) || h < 90) ? 90 : h;
    }

    /** 展开甬道：卡片被顶到顶部 */
    function openTunnels() {
        row.classList.add('organized');
        var h = tunnelHeight();
        row.querySelectorAll('.tunnel').forEach(function (tunnel) {
            tunnel.style.height = h + 'px';
        });
    }

    /** 收起甬道：卡片回到舞台底部 */
    function closeTunnels() {
        row.querySelectorAll('.tunnel').forEach(function (tunnel) {
            tunnel.style.height = '0px';
        });
        row.classList.remove('organized');
    }

    /* ==================== 气泡 ==================== */

    function createBubbleEl(answer) {
        var el = document.createElement('div');
        el.className = 'bubble ' + PALETTE[(answer.id || 0) % PALETTE.length];
        el.textContent = answer.expression;
        el.title = (answer.no ? '第 ' + answer.no + ' 题' : '')
            + (answer.time ? ' · ' + answer.time + ' 提交' : '');
        el.setAttribute('data-qid', answer.qid);
        field.appendChild(el);
        return el;
    }

    function slotLayout() {
        var size = fieldSize();
        var n = bubbles.length;
        if (!n) return;
        var cols = Math.max(2, Math.min(6,
            Math.round(Math.sqrt(n * size.w / Math.max(size.h * 1.35, 1)))));
        var rows = Math.ceil(n / cols);
        var cellW = size.w / cols;
        var cellH = size.h / rows;

        bubbles.forEach(function (b, i) {
            b.w = b.el.offsetWidth;
            b.h = b.el.offsetHeight;
            var c = i % cols;
            var r = Math.floor(i / cols);
            b.x = clamp(c * cellW + (cellW - b.w) / 2 + rand(-10, 10), 2, Math.max(2, size.w - b.w - 2));
            b.y = clamp(r * cellH + (cellH - b.h) / 2 + rand(-8, 8), 2, Math.max(2, size.h - b.h - 2));
            b.vx = rand(0.1, 0.26) * (Math.random() < 0.5 ? -1 : 1);
            b.vy = rand(0.06, 0.16) * (Math.random() < 0.5 ? -1 : 1);
            render(b);
        });
    }

    function render(b) {
        b.el.style.transform = 'translate3d(' + b.x.toFixed(1) + 'px,' + b.y.toFixed(1) + 'px,0)';
    }

    function buildAllBubbles() {
        bubbles.forEach(function (b) { b.el.remove(); });
        bubbles = [];
        answers.forEach(function (a) {
            bubbles.push({ a: a, el: createBubbleEl(a), x: 0, y: 0, w: 0, h: 0, vx: 0, vy: 0 });
        });
        updateDensity();
        slotLayout();
    }

    /** 轮询新增的算式 */
    function addBubbles(items) {
        if (!items || !items.length) return;
        if (busy) {
            pendingItems = pendingItems.concat(items);
            return;
        }

        if (isOrganized()) {
            // 已经归类：新算式直接飞进对应甬道，页面保持归类状态
            addAnswersOrganized(items);
            return;
        }

        var size = fieldSize();
        items.forEach(function (item) {
            var answer = {
                id: parseInt(item.id, 10) || 0,
                qid: String(item.questionId),
                expression: item.expression,
                time: item.time || '',
                no: (byQid[String(item.questionId)]
                    ? Array.prototype.indexOf.call(row.children, byQid[String(item.questionId)]) + 1
                    : '')
            };
            if (answers.some(function (x) { return x.id === answer.id; })) return;
            answers.push(answer);

            var el = createBubbleEl(answer);
            el.classList.add('fresh');
            var b = {
                a: answer, el: el,
                x: rand(10, Math.max(10, size.w - 120)),
                y: rand(10, Math.max(10, size.h - 40)),
                w: el.offsetWidth, h: el.offsetHeight,
                vx: rand(0.1, 0.26) * (Math.random() < 0.5 ? -1 : 1),
                vy: rand(0.06, 0.16) * (Math.random() < 0.5 ? -1 : 1)
            };
            bubbles.push(b);
            render(b);
        });
        updateDensity();

        if (fieldEmpty) fieldEmpty.style.display = 'none';
        btn.disabled = false;
        if (btnClear) btnClear.disabled = false;
        if (!drifting) startDrift();
    }

    /** 归类状态下新增的算式：从舞台顶部飞入对应格式栏，落地成 chip */
    function flyIntoTunnel(answer, delay) {
        var lane = laneOf(answer);
        if (!lane) return;

        var stageRect = stage.getBoundingClientRect();
        var laneRect = laneColFor(lane).getBoundingClientRect();
        var startX = laneRect.left - stageRect.left + laneRect.width / 2 + rand(-80, 80);
        var startY = 58 + rand(0, 26);
        var targetX = laneRect.left - stageRect.left + laneRect.width / 2;
        var targetY = laneRect.bottom - stageRect.top - 22;
        var midX = startX + (targetX - startX) * 0.5;
        var midY = startY + (targetY - startY) * 0.35 - 18;

        var el = document.createElement('div');
        el.className = 'fly-chip';
        el.textContent = answer.expression;
        // 跟随目标甬道的主题色（上栏/下栏分别用 a/b 两套色）
        var col = byQid[String(answer.qid)];
        if (col) {
            var cs = getComputedStyle(col);
            var kind = String(answer.expression).indexOf('(') >= 0 ? 'a' : 'b';
            var c = cs.getPropertyValue('--chip-' + kind + '-c').trim();
            var b = cs.getPropertyValue('--chip-' + kind + '-b').trim();
            var bg = cs.getPropertyValue('--chip-' + kind + '-bg').trim();
            if (c) el.style.color = c;
            if (b) el.style.borderColor = b;
            if (bg) { el.style.background = bg; el.style.boxShadow = '0 0 14px ' + bg; }
        }
        stage.appendChild(el);

        var anim = el.animate([
            { transform: 'translate3d(' + startX + 'px,' + startY + 'px,0) scale(.85)', opacity: 0 },
            { transform: 'translate3d(' + midX + 'px,' + midY + 'px,0) scale(1.06)', opacity: 1, offset: 0.4 },
            { transform: 'translate3d(' + targetX + 'px,' + targetY + 'px,0) scale(.9)', opacity: 1 }
        ], {
            duration: 460,
            delay: delay || 0,
            easing: 'cubic-bezier(.3,.8,.3,1)',
            fill: 'forwards'
        });
        anim.onfinish = function () {
            el.remove();
            addChip(answer);
        };
    }

    /** 归类状态下接收新提交的算式（轮询到达） */
    function addAnswersOrganized(items) {
        if (!items || !items.length) return;
        var queued = 0;
        items.forEach(function (item) {
            var answer = {
                id: parseInt(item.id, 10) || 0,
                qid: String(item.questionId),
                expression: item.expression,
                time: item.time || '',
                no: ''
            };
            if (answers.some(function (x) { return x.id === answer.id; })) return;
            answers.push(answer);
            flyIntoTunnel(answer, queued * 140);
            queued++;
        });
        if (!queued) return;

        updateDensity();
        if (fieldEmpty) fieldEmpty.style.display = 'none';
        cntTotal.textContent = answers.length;
        btn.disabled = false;
        if (btnClear) btnClear.disabled = false;
    }

    /* ==================== 游动 ==================== */

    function tick() {
        var size = fieldSize();
        var i;
        var b;

        for (i = 0; i < bubbles.length; i++) {
            b = bubbles[i];
            b.x += b.vx;
            b.y += b.vy;
            if (b.x <= 0 || b.x >= size.w - b.w) {
                b.vx *= -1;
                b.x = clamp(b.x, 0, Math.max(0, size.w - b.w));
            }
            if (b.y <= 0 || b.y >= size.h - b.h) {
                b.vy *= -1;
                b.y = clamp(b.y, 0, Math.max(0, size.h - b.h));
            }
        }

        // 相互推挤：两气泡靠太近时沿重叠较小的轴分开
        for (var pass = 0; pass < 3; pass++) {
            for (i = 0; i < bubbles.length; i++) {
                for (var j = i + 1; j < bubbles.length; j++) {
                    var a = bubbles[i];
                    var c = bubbles[j];
                    var ox = (a.w + c.w) / 2 + 10 - Math.abs((a.x + a.w / 2) - (c.x + c.w / 2));
                    var oy = (a.h + c.h) / 2 + 6 - Math.abs((a.y + a.h / 2) - (c.y + c.h / 2));
                    if (ox > 0 && oy > 0) {
                        if (ox < oy) {
                            var sx = (a.x + a.w / 2) < (c.x + c.w / 2) ? -1 : 1;
                            a.x += sx * ox * 0.35;
                            c.x -= sx * ox * 0.35;
                        } else {
                            var sy = (a.y + a.h / 2) < (c.y + c.h / 2) ? -1 : 1;
                            a.y += sy * oy * 0.35;
                            c.y -= sy * oy * 0.35;
                        }
                        a.x = clamp(a.x, 0, Math.max(0, size.w - a.w));
                        c.x = clamp(c.x, 0, Math.max(0, size.w - c.w));
                        a.y = clamp(a.y, 0, Math.max(0, size.h - a.h));
                        c.y = clamp(c.y, 0, Math.max(0, size.h - c.h));
                    }
                }
            }
        }

        for (i = 0; i < bubbles.length; i++) {
            render(bubbles[i]);
        }
        rafId = requestAnimationFrame(tick);
    }

    function startDrift() {
        if (drifting) return;
        drifting = true;
        rafId = requestAnimationFrame(tick);
    }

    function stopDrift() {
        drifting = false;
        if (rafId) {
            cancelAnimationFrame(rafId);
            rafId = null;
        }
    }

    /* ==================== 甬道 chip ==================== */

    /** 按算式结构选择分栏：含括号 → 上方「A×(B+C)」；否则 → 下方「A×B＋A×C」 */
    function laneOf(answer) {
        var col = byQid[String(answer.qid)];
        if (!col) return null;
        var kind = String(answer.expression).indexOf('(') >= 0 ? 'paren' : 'expand';
        return col.querySelector('.lane[data-kind="' + kind + '"]');
    }

    /** 两列布局：优先放进条目较少的那一列 */
    function laneColFor(lane) {
        var cols = lane.querySelectorAll('.lane-col');
        if (!cols.length) return lane;
        if (cols.length < 2) return cols[0];
        return cols[0].children.length <= cols[1].children.length ? cols[0] : cols[1];
    }

    function addChip(answer) {
        var lane = laneOf(answer);
        if (!lane) return;
        var chip = document.createElement('div');
        chip.className = 'chip';

        var text = document.createElement('span');
        text.textContent = answer.expression;
        chip.appendChild(text);

        var del = document.createElement('button');
        del.type = 'button';
        del.className = 'chip-del';
        del.title = '删除这条算式';
        del.textContent = '✕';
        del.setAttribute('data-answer-id', answer.id);
        chip.appendChild(del);

        laneColFor(lane).appendChild(chip);
        refreshCounts();
    }

    function refreshCounts() {
        Object.keys(byQid).forEach(function (qid) {
            var col = byQid[qid];
            var total = col.querySelectorAll('.lane .chip').length;
            var num = col.querySelector('.num');
            if (num) num.textContent = total;
        });
    }

    /* ---------- AJAX 轻提示 ---------- */
    var ajaxToast = document.getElementById('ajaxToast');
    var ajaxToastTimer = null;

    function showAjaxToast(text, isError) {
        if (!ajaxToast) return;
        ajaxToast.textContent = text;
        ajaxToast.className = 'ajax-toast show' + (isError ? ' error' : '');
        clearTimeout(ajaxToastTimer);
        ajaxToastTimer = setTimeout(function () {
            ajaxToast.className = 'ajax-toast' + (isError ? ' error' : '');
        }, 2200);
    }

    /** 删除单条算式：AJAX 删除 + 原地移除 chip，不刷新页面 */
    function deleteAnswerChip(delBtn) {
        var answerId = delBtn.getAttribute('data-answer-id');
        if (!answerId) return;
        var chip = delBtn.closest ? delBtn.closest('.chip') : null;

        var meta = document.querySelector('meta[name="_csrf"]');
        var headerMeta = document.querySelector('meta[name="_csrf_header"]');
        var token = meta ? meta.getAttribute('content') : '';
        var headerName = headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN';
        var ctx = body.getAttribute('data-ctx') || '/';
        if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';

        var headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        headers[headerName] = token;

        delBtn.disabled = true;
        fetch(ctx + 'distributive/admin/answer/delete-json', {
            method: 'POST',
            credentials: 'same-origin',
            headers: headers,
            body: 'id=' + encodeURIComponent(answerId)
        }).then(function (resp) {
            return resp.json();
        }).then(function (data) {
            if (!data || !data.success) {
                delBtn.disabled = false;
                showAjaxToast((data && data.message) || '删除失败，稍后再试', true);
                return;
            }
            answers = answers.filter(function (a) {
                return String(a.id) !== String(answerId);
            });
            if (chip) {
                chip.classList.add('chip-out');
                setTimeout(function () {
                    chip.remove();
                    refreshCounts();
                }, 230);
            } else {
                refreshCounts();
            }
            cntTotal.textContent = Math.max(0, answers.length);
            if (!answers.length) {
                btn.disabled = true;
                if (btnClear) btnClear.disabled = true;
            }
            showAjaxToast(data.message || '已删除');
        }).catch(function () {
            delBtn.disabled = false;
            showAjaxToast('网络开小差了，稍后再试', true);
        });
    }

    /* ==================== 开始归类 / 再次归类 ==================== */

    /** 阶段 A：卡片上移的同时，把所有气泡收拢到卡片下方的待飞区，避免和图片叠加 */
    function stageBubblesIntoTunnels() {
        var stageRect = stage.getBoundingClientRect();
        var rowRect = row.getBoundingClientRect();
        var fieldRect = field.getBoundingClientRect();
        var bandTop = rowRect.top - stageRect.top + cardHeight() + 14;
        var bandBottom = stageRect.height - 22;
        var available = Math.max(80, bandBottom - bandTop);
        var n = bubbles.length;
        // 用接近正方形的小格子铺开，行距固定 52px，数量多时自动向甬道深处延伸
        var cols = Math.max(3, Math.min(8, Math.ceil(Math.sqrt(n))));
        var rows = Math.max(1, Math.ceil(n / cols));
        var bandH = Math.min(available, rows * 52);
        var cellW = stageRect.width / cols;
        var cellH = bandH / rows;

        bubbles.forEach(function (b, i) {
            var c = i % cols;
            var r = Math.floor(i / cols);
            var cx = c * cellW + cellW / 2;
            var cy = bandTop + r * cellH + cellH / 2;
            var tx = cx - fieldRect.left - b.w / 2;
            var ty = cy - fieldRect.top - b.h / 2;

            var anim = b.el.animate([
                { transform: 'translate3d(' + b.x + 'px,' + b.y + 'px,0) scale(1)', opacity: 1 },
                { transform: 'translate3d(' + tx + 'px,' + ty + 'px,0) scale(.9)', opacity: 1 }
            ], {
                duration: 420,
                easing: 'cubic-bezier(.3,.85,.35,1)',
                fill: 'forwards'
            });
            anim.onfinish = (function (bubble, ax, ay, animation) {
                return function () {
                    bubble.x = ax;
                    bubble.y = ay;
                    bubble.el.style.transform = 'translate3d(' + ax + 'px,' + ay + 'px,0)';
                    if (animation && animation.cancel) animation.cancel();
                };
            })(b, tx, ty, anim);
        });
    }

    /** 阶段 B：从待飞区一个一个飞入对应格式栏 */
    function flyBubblesIntoHoles() {
        var targets = {};
        function targetFor(answer) {
            var lane = laneOf(answer);
            if (!lane) return null;
            var key = String(answer.qid) + '|' + lane.getAttribute('data-kind');
            if (!targets[key]) {
                var rect = lane.getBoundingClientRect();
                targets[key] = { x: rect.left + rect.width / 2, y: rect.bottom - 16 };
            }
            return targets[key];
        }

        // 数量多时自动加快节奏，避免几十条算式飞太久
        var total = bubbles.length;
        var stagger = total > 40 ? 55 : (total > 24 ? 80 : 110);
        var flyDur = total > 40 ? 360 : 420;

        var delay = 0;
        bubbles.forEach(function (b, index) {
            var target = targetFor(b.a);
            if (!target) return;

            var rect = b.el.getBoundingClientRect();
            var dx = target.x - (rect.left + rect.width / 2);
            var dy = target.y - (rect.top + rect.height / 2);
            var startX = b.x;
            var startY = b.y;
            var endX = startX + dx;
            var endY = startY + dy;
            var itemDelay = index * stagger;

            var anim = b.el.animate([
                { transform: 'translate3d(' + startX + 'px,' + startY + 'px,0) scale(1)', opacity: 1 },
                { transform: 'translate3d(' + (startX + dx * 0.16) + 'px,' + (startY + dy * 0.45 - 14) + 'px,0) scale(1.06)', opacity: 1, offset: 0.4 },
                { transform: 'translate3d(' + endX + 'px,' + endY + 'px,0) scale(.58)', opacity: .12 }
            ], {
                duration: flyDur,
                delay: itemDelay,
                easing: 'cubic-bezier(.35,.6,.3,1)',
                fill: 'forwards'
            });
            delay = Math.max(delay, itemDelay);

            anim.onfinish = (function (bubble) {
                return function () {
                    bubble.el.remove();
                    addChip(bubble.a);
                };
            })(b);
        });

        setTimeout(finishConverge, delay + flyDur + 160);
    }

    function converge() {
        if (busy || !bubbles.length) return;
        busy = true;
        stopDrift();
        btn.disabled = true;
        btn.textContent = '✨ 正在归类…';
        doneFlag.hidden = true;

        stage.classList.add('is-flying');
        openTunnels();

        if (reducedMotion) {
            bubbles.forEach(function (b) { b.el.remove(); addChip(b.a); });
            bubbles = [];
            setTimeout(finishConverge, 120);
            return;
        }

        // 阶段 A：照片上移 + 算式整体先落到卡片下方的甬道区域
        stageBubblesIntoTunnels();

        // 阶段 B：等卡片/甬道就位后，再一个一个飞入对应分栏
        setTimeout(flyBubblesIntoHoles, Math.max(TUNNEL_TRANSITION, 470) + 40);
    }
    function finishConverge() {
        bubbles = [];
        field.style.visibility = 'hidden';
        stage.classList.remove('is-flying');
        doneFlag.hidden = false;
        btn.disabled = answers.length === 0;
        btn.textContent = '🔄 再次归类';
        btn.classList.add('is-reset');
        busy = false;

        if (pendingItems.length) {
            var queued = pendingItems;
            pendingItems = [];
            addBubbles(queued);
        }
    }

    function resetConverge() {
        if (busy) return;
        btn.classList.remove('is-reset');
        btn.textContent = '🧩 开始归类';
        btn.disabled = answers.length === 0;
        doneFlag.hidden = true;

        row.querySelectorAll('.lane-col').forEach(function (col) { col.innerHTML = ''; });
        refreshCounts();

        stage.classList.add('is-flying');
        closeTunnels();
        field.style.visibility = '';
        buildAllBubbles();
        startDrift();
        setTimeout(function () {
            layoutField();
            stage.classList.remove('is-flying');
        }, TUNNEL_TRANSITION);
    }

    btn.addEventListener('click', function () {
        if (btn.classList.contains('is-reset')) {
            resetConverge();
        } else {
            converge();
        }
    });

    /* ==================== 轮询（固定 15 秒 + 手动刷新） ==================== */

    function poll() {
        if (document.hidden) return;
        var ctx = body.getAttribute('data-ctx') || '/';
        if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';
        var since = Number(content.getAttribute('data-max-id') || 0);

        fetch(ctx + 'distributive/admin/answers?sinceId=' + since, { credentials: 'same-origin' })
            .then(function (resp) { return resp.json(); })
            .then(function (data) {
                if (!data || !data.success) return;
                content.setAttribute('data-max-id', data.maxId);
                cntTotal.textContent = data.total;
                if (!data.items || !data.items.length) return;
                addBubbles(data.items);
            })
            .catch(function () { /* 网络抖动忽略，下次再来 */ });
    }

    /* ==================== 擦除所有算式 ==================== */

    function openClearConfirm() {
        if (busy || !answers.length) return;
        clearMask.classList.add('open');
        clearMask.setAttribute('aria-hidden', 'false');
    }

    function closeClearConfirm() {
        if (!clearMask) return;
        clearMask.classList.remove('open');
        clearMask.setAttribute('aria-hidden', 'true');
    }

    function doClearAll() {
        if (busy || !answers.length) return;

        var meta = document.querySelector('meta[name="_csrf"]');
        var headerMeta = document.querySelector('meta[name="_csrf_header"]');
        var token = meta ? meta.getAttribute('content') : '';
        var headerName = headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN';
        var ctx = body.getAttribute('data-ctx') || '/';
        if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';

        var headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        headers[headerName] = token;

        btnClear.disabled = true;
        btnClear.textContent = '🧹 擦除中…';

        fetch(ctx + 'distributive/admin/answer/clear-json', {
            method: 'POST',
            credentials: 'same-origin',
            headers: headers
        }).then(function (resp) {
            return resp.json();
        }).then(function (data) {
            if (!data || !data.success) {
                showAjaxToast((data && data.message) || '擦除失败，稍后再试', true);
                btnClear.disabled = false;
                btnClear.textContent = '🧹 擦除所有算式';
                return;
            }

            // 清空本地状态与界面，不刷新页面
            stopDrift();
            bubbles.forEach(function (b) { b.el.remove(); });
            bubbles = [];
            pendingItems = [];
            row.querySelectorAll('.lane-col').forEach(function (col) { col.innerHTML = ''; });
            answers = [];
            refreshCounts();
            cntTotal.textContent = '0';
            doneFlag.hidden = true;
            stage.classList.remove('is-flying');
            closeTunnels();
            field.style.visibility = '';
            btn.classList.remove('is-reset');
            btn.textContent = '🧩 开始归类';
            btn.disabled = true;
            layoutField();
            if (fieldEmpty) fieldEmpty.style.display = '';
            btnClear.disabled = true;
            btnClear.textContent = '🧹 擦除所有算式';
            showAjaxToast(data.message || '已擦除所有算式');
        }).catch(function () {
            showAjaxToast('网络开小差了，稍后再试', true);
            btnClear.disabled = false;
            btnClear.textContent = '🧹 擦除所有算式';
        });
    }

    // 手动刷新：整页重新加载，回到「未归类」的初始状态
    if (btnClear) {
        btnClear.addEventListener('click', openClearConfirm);
    }
    if (clearConfirm) {
        clearConfirm.addEventListener('click', function () {
            closeClearConfirm();
            doClearAll();
        });
    }
    if (clearCancel) {
        clearCancel.addEventListener('click', closeClearConfirm);
    }
    if (clearMask) {
        clearMask.addEventListener('click', function (e) {
            if (e.target === clearMask) closeClearConfirm();
        });
    }
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && clearMask && clearMask.classList.contains('open')) {
            closeClearConfirm();
        }
    });

    if (btnRefresh) {
        btnRefresh.addEventListener('click', function () {
            var ctx = body.getAttribute('data-ctx') || '/';
            if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';
            window.location.href = ctx + 'distributive/admin/list';
        });
    }

    // 固定 15 秒自动刷新，不再提供开关
    pollState.textContent = '自动刷新 15s';
    pollTimer = setInterval(poll, POLL_INTERVAL);

    document.addEventListener('visibilitychange', function () {
        if (document.hidden) {
            stopDrift();
        } else {
            if (bubbles.length && !busy && !isOrganized()) startDrift();
            poll();
        }
    });

    /* ==================== 删除 chip ==================== */

    row.addEventListener('click', function (e) {
        var delBtn = e.target.closest ? e.target.closest('.chip-del') : null;
        if (!delBtn) return;
        e.preventDefault();
        deleteAnswerChip(delBtn);
    });

    /* ==================== 初始化 ==================== */

    function init() {
        layoutField();
        buildAllBubbles();
        updateDensity();
        btn.disabled = answers.length === 0;
        if (answers.length) {
            if (fieldEmpty) fieldEmpty.style.display = 'none';
            startDrift();
        }
    }

    var resizeTimer = null;
    window.addEventListener('resize', function () {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(function () {
            if (isOrganized()) {
                var h = tunnelHeight();
                row.querySelectorAll('.tunnel').forEach(function (tunnel) {
                    tunnel.style.height = h + 'px';
                });
                return;
            }
            layoutField();
            var size = fieldSize();
            bubbles.forEach(function (b) {
                b.x = clamp(b.x, 0, Math.max(0, size.w - b.w));
                b.y = clamp(b.y, 0, Math.max(0, size.h - b.h));
                render(b);
            });
        }, 160);
    });

    init();
})();

