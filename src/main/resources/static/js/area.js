(function () {
    'use strict';

    var MIN_LEN = 2, MAX_LEN = 10;
    var MIN_WID = 2, MAX_WID = 10;
    var STEP = 0.5;

    var state = { len: 8, wid: 8, mid: 4 };

    var stage = document.getElementById('areaStage');

    var lenRange = document.getElementById('lenRange');
    var widRange = document.getElementById('widRange');
    var midRange = document.getElementById('midRange');

    var lenVal = document.getElementById('lenVal');
    var widVal = document.getElementById('widVal');
    var midVal = document.getElementById('midVal');

    var leftAreaEl = document.getElementById('leftArea');
    var totalAreaEl = document.getElementById('totalArea');
    var rightAreaEl = document.getElementById('rightArea');

    var btnReset = document.getElementById('btnReset');
    var btnHalf = document.getElementById('btnHalf');

    function clamp(v, min, max) {
        return Math.min(max, Math.max(min, v));
    }

    function fmt(v) {
        var r = Math.round(v * 100) / 100;
        return String(r);
    }

    function clampMiddle() {
        var maxMid = state.len - STEP;
        state.mid = clamp(state.mid, STEP, maxMid);
        midRange.min = String(STEP);
        midRange.max = String(maxMid);
    }

    function svgMarkup() {
        var len = state.len;
        var wid = state.wid;
        var mid = state.mid;

        // 固定比例尺：参数越大图形越大（10 cm 对应 240 px，正好放下）
        var scale = 30;
        var rw = len * scale;
        var rh = wid * scale;
        var x0 = (560 - rw) / 2;
        var y0 = 80;
        var splitX = x0 + mid * scale;
        var rightW = rw - mid * scale;

        var leftPx = splitX - x0;
        var showLeftLabel = leftPx > 58;
        var showRightLabel = rightW > 58;

        var parts = [];
        parts.push('<svg viewBox="0 0 560 460" role="img" aria-label="面积割补示意图">');

        parts.push('<defs>');
        parts.push('<marker id="arrBlue" markerWidth="8" markerHeight="8" refX="6" refY="4" orient="auto">'
            + '<path d="M0,0 L8,4 L0,8 z" fill="#4a8bd6"/></marker>');
        parts.push('<marker id="arrOrange" markerWidth="8" markerHeight="8" refX="6" refY="4" orient="auto">'
            + '<path d="M0,0 L8,4 L0,8 z" fill="#ea8a55"/></marker>');
        parts.push('</defs>');

        var topY = y0 - 26;
        parts.push('<line class="dim-line left" x1="' + x0 + '" y1="' + topY + '" x2="' + splitX + '" y2="' + topY
            + '" marker-end="url(#arrBlue)"/>');
        parts.push('<line class="dim-line left" x1="' + splitX + '" y1="' + topY + '" x2="' + x0 + '" y2="' + topY + '"/>');
        if (rightW > 4) {
            parts.push('<line class="dim-line right" x1="' + splitX + '" y1="' + topY + '" x2="' + (x0 + rw) + '" y2="' + topY
                + '" marker-end="url(#arrOrange)"/>');
            parts.push('<line class="dim-line right" x1="' + (x0 + rw) + '" y1="' + topY + '" x2="' + splitX + '" y2="' + topY + '"/>');
        }

        parts.push('<text class="dim-text left" x="' + ((x0 + splitX) / 2) + '" y="' + (topY - 12) + '" text-anchor="middle">'
            + fmt(mid) + ' cm</text>');
        if (showRightLabel) {
            parts.push('<text class="dim-text right" x="' + ((splitX + x0 + rw) / 2) + '" y="' + (topY - 12) + '" text-anchor="middle">'
                + fmt(len - mid) + ' cm</text>');
        }

        parts.push('<rect x="' + x0 + '" y="' + y0 + '" width="' + (mid * scale) + '" height="' + rh
            + '" fill="#8fc0e8" stroke="#2c3e50" stroke-width="2"/>');
        parts.push('<rect x="' + splitX + '" y="' + y0 + '" width="' + rightW + '" height="' + rh
            + '" fill="#f6b08c" stroke="#2c3e50" stroke-width="2"/>');
        parts.push('<line x1="' + splitX + '" y1="' + y0 + '" x2="' + splitX + '" y2="' + (y0 + rh)
            + '" stroke="#ffffff" stroke-width="3" stroke-dasharray="8 7"/>');

        if (showLeftLabel) {
            parts.push('<text class="inside-text" x="' + ((x0 + splitX) / 2) + '" y="' + (y0 + rh - 14)
                + '" text-anchor="middle">左 ' + fmt(mid) + ' cm</text>');
        }
        if (showRightLabel) {
            parts.push('<text class="inside-text" x="' + ((splitX + x0 + rw) / 2) + '" y="' + (y0 + rh - 14)
                + '" text-anchor="middle">右 ' + fmt(len - mid) + ' cm</text>');
        }

        var wX = x0 - 26;
        parts.push('<line class="dim-line" x1="' + wX + '" y1="' + y0 + '" x2="' + wX + '" y2="' + (y0 + rh) + '"/>');
        parts.push('<line class="dim-line" x1="' + (wX - 6) + '" y1="' + y0 + '" x2="' + (wX + 6) + '" y2="' + y0 + '"/>');
        parts.push('<line class="dim-line" x1="' + (wX - 6) + '" y1="' + (y0 + rh) + '" x2="' + (wX + 6) + '" y2="' + (y0 + rh) + '"/>');
        parts.push('<text class="other-text" x="' + (wX - 12) + '" y="' + (y0 + rh / 2) + '" text-anchor="middle"'
            + ' transform="rotate(-90 ' + (wX - 12) + ' ' + (y0 + rh / 2) + ')">宽 ' + fmt(wid) + ' cm</text>');

        var bY = y0 + rh + 22;
        parts.push('<line class="dim-line" x1="' + x0 + '" y1="' + bY + '" x2="' + (x0 + rw) + '" y2="' + bY + '"/>');
        parts.push('<line class="dim-line" x1="' + x0 + '" y1="' + (bY - 6) + '" x2="' + x0 + '" y2="' + (bY + 6) + '"/>');
        parts.push('<line class="dim-line" x1="' + (x0 + rw) + '" y1="' + (bY - 6) + '" x2="' + (x0 + rw) + '" y2="' + (bY + 6) + '"/>');
        parts.push('<text class="other-text" x="' + (x0 + rw / 2) + '" y="' + (bY + 30) + '" text-anchor="middle">总长 '
            + fmt(len) + ' cm</text>');

        parts.push('</svg>');
        return parts.join('');
    }

    function render() {
        clampMiddle();

        lenVal.textContent = fmt(state.len) + ' cm';
        widVal.textContent = fmt(state.wid) + ' cm';
        midVal.textContent = fmt(state.mid) + ' cm';

        var left = state.mid * state.wid;
        var right = (state.len - state.mid) * state.wid;
        var total = state.len * state.wid;

        leftAreaEl.textContent = fmt(left);
        rightAreaEl.textContent = fmt(right);
        totalAreaEl.textContent = fmt(total);

        stage.innerHTML = svgMarkup();
    }

    function bindRange(el, key) {
        if (!el) return;
        el.addEventListener('input', function () {
            var v = parseFloat(el.value);
            if (isNaN(v)) return;
            state[key] = v;
            render();
        });
    }

    bindRange(lenRange, 'len');
    bindRange(widRange, 'wid');
    bindRange(midRange, 'mid');

    if (btnReset) {
        btnReset.addEventListener('click', function () {
            state.len = 8;
            state.wid = 8;
            state.mid = 4;
            lenRange.value = String(state.len);
            widRange.value = String(state.wid);
            midRange.value = String(state.mid);
            render();
        });
    }

    if (btnHalf) {
        btnHalf.addEventListener('click', function () {
            state.mid = state.len / 2;
            midRange.value = String(state.mid);
            render();
        });
    }

    if (lenRange) lenRange.min = String(MIN_LEN);
    if (lenRange) lenRange.max = String(MAX_LEN);
    if (widRange) widRange.min = String(MIN_WID);
    if (widRange) widRange.max = String(MAX_WID);

    render();
})();
