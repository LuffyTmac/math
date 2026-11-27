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
    function headers() {
        var h = { 'Content-Type': 'application/json;charset=UTF-8' };
        h[csrfHeader()] = csrfToken();
        return h;
    }

    /* ==================== 列表页：删除 ==================== */
    var delButtons = document.querySelectorAll('.js-del');
    if (delButtons.length) {
        delButtons.forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (!window.confirm('确定删除这道题吗？')) return;
                var id = Number(btn.getAttribute('data-id'));
                fetch(ctx + 'variation/admin/delete', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: headers(),
                    body: JSON.stringify({ id: id })
                }).then(function (r) { return r.json(); })
                  .then(function (data) {
                      if (data && data.success) {
                          var row = btn.closest('tr');
                          if (row) row.parentNode.removeChild(row);
                      } else {
                          alert((data && data.message) || '删除失败');
                      }
                  })
                  .catch(function () { alert('删除失败，稍后再试～'); });
            });
        });
    }

    /* ==================== 居中提示 ==================== */
    var mask = document.getElementById('varMask');
    var modalEmoji = document.getElementById('varModalEmoji');
    var modalText = document.getElementById('varModalText');
    var modalOk = document.getElementById('varModalOk');
    var modalTimer = null;

    function showModal(emoji, text) {
        if (!mask) return;
        modalEmoji.textContent = emoji || '✅';
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

    /* ==================== 编辑页 ==================== */
    var editData = document.getElementById('editData');
    if (!editData) return;

    var state = {
        id: editData.getAttribute('data-id') ? Number(editData.getAttribute('data-id')) : null,
        type: editData.getAttribute('data-type') || '',
        content: editData.getAttribute('data-content') || '',
        answers: [],
        judge: editData.getAttribute('data-judge') || '',
        options: [],
        correct: 0,
        sort: Number(editData.getAttribute('data-sort') || 0)
    };
    try {
        state.answers = JSON.parse(editData.getAttribute('data-answers') || '[]');
    } catch (e) {
        state.answers = [];
    }
    try {
        state.options = JSON.parse(editData.getAttribute('data-options') || '[]');
    } catch (e) {
        state.options = [];
    }
    state.options = state.options.map(function (o) {
        return Array.isArray(o) ? o.slice() : [String(o)];
    });
    state.correct = Number(editData.getAttribute('data-correct') || 0);
    if (state.type === 'CHOICE' && state.options.length) {
        state.answers = (state.options[state.correct] || []).slice();
    }
    while (state.options.length < 4 && state.type === 'CHOICE') {
        state.options.push([]);
    }

    var tabs = document.querySelectorAll('.type-tab');
    var builder = document.getElementById('builder');
    var qContent = document.getElementById('qContent');
    var keypad = document.getElementById('keypad');
    var fillPanel = document.getElementById('fillPanel');
    var judgePanel = document.getElementById('judgePanel');
    var choicePanel = document.getElementById('choicePanel');
    var choiceOptionsEl = document.getElementById('choiceOptions');
    var choiceAnswersEl = document.getElementById('choiceAnswers');
    var btnGenOptions = document.getElementById('btnGenOptions');
    var answersEl = document.getElementById('answers');
    var sortInput = document.getElementById('sortOrder');
    var btnSave = document.getElementById('btnSave');
    var saveMsg = document.getElementById('saveMsg');
    var blankKey = keypad ? keypad.querySelector('.blank-key') : null;

    function blankCount() {
        var m = state.content.match(/□/g);
        return m ? m.length : 0;
    }

    function readAnswerInputs() {
        if (!answersEl) return;
        var inputs = answersEl.querySelectorAll('input');
        for (var i = 0; i < inputs.length; i++) {
            state.answers[i] = inputs[i].value;
        }
    }

    function renderAnswers() {
        if (!answersEl) return;
        readAnswerInputs();
        var count = blankCount();
        var values = [];
        for (var i = 0; i < count; i++) {
            values.push(state.answers[i] || '');
        }
        state.answers = values;

        answersEl.innerHTML = '';
        for (var j = 0; j < count; j++) {
            var wrap = document.createElement('label');
            wrap.className = 'answer-item';
            var span = document.createElement('span');
            span.textContent = '第 ' + (j + 1) + ' 空';
            var input = document.createElement('input');
            input.type = 'text';
            input.value = values[j];
            input.maxLength = 8;
            input.setAttribute('data-index', String(j));
            input.addEventListener('input', function () {
                state.answers[Number(this.getAttribute('data-index'))] = this.value;
            });
            wrap.appendChild(span);
            wrap.appendChild(input);
            answersEl.appendChild(wrap);
        }
    }

    function renderContent() {
        if (!qContent) return;
        qContent.innerHTML = '';
        var parts = state.content.split('□');
        parts.forEach(function (part, i) {
            if (part) qContent.appendChild(document.createTextNode(part));
            if (i < parts.length - 1) {
                var box = document.createElement('span');
                box.className = 'blank-box';
                qContent.appendChild(box);
            }
        });
        qContent.classList.toggle('is-empty', !state.content);
    }

    function readChoiceAnswers() {
        if (!choiceAnswersEl) return;
        var inputs = choiceAnswersEl.querySelectorAll('input');
        for (var i = 0; i < inputs.length; i++) {
            state.answers[i] = inputs[i].value;
        }
    }

    function renderChoiceAnswers() {
        if (!choiceAnswersEl) return;
        readChoiceAnswers();
        var count = blankCount();
        var values = [];
        for (var i = 0; i < count; i++) {
            values.push(state.answers[i] || '');
        }
        state.answers = values;

        choiceAnswersEl.innerHTML = '';
        for (var j = 0; j < count; j++) {
            var wrap = document.createElement('label');
            wrap.className = 'answer-item';
            var span = document.createElement('span');
            span.textContent = '第 ' + (j + 1) + ' 空';
            var input = document.createElement('input');
            input.type = 'text';
            input.value = values[j];
            input.maxLength = 8;
            input.setAttribute('data-index', String(j));
            input.addEventListener('input', function () {
                state.answers[Number(this.getAttribute('data-index'))] = this.value;
            });
            wrap.appendChild(span);
            wrap.appendChild(input);
            choiceAnswersEl.appendChild(wrap);
        }
    }

    function renderChoiceOptions() {
        if (!choiceOptionsEl) return;
        choiceOptionsEl.innerHTML = '';
        state.options.forEach(function (opt, i) {
            var row = document.createElement('label');
            row.className = 'choice-option-row';

            var radio = document.createElement('input');
            radio.type = 'radio';
            radio.name = 'choiceCorrect';
            radio.value = String(i);
            radio.checked = i === state.correct;
            radio.addEventListener('change', function () {
                state.correct = Number(this.value);
            });

            var letter = document.createElement('span');
            letter.className = 'opt-letter';
            letter.textContent = String.fromCharCode(65 + i);

            var input = document.createElement('input');
            input.type = 'text';
            input.className = 'opt-input';
            input.maxLength = 20;
            input.value = Array.isArray(opt) ? opt.join('、') : String(opt);
            input.addEventListener('input', function () {
                var idx = Number(this.getAttribute('data-index'));
                var parts = this.value.split(/[、,，\\s]+/).filter(function (p) { return p !== ''; });
                state.options[idx] = parts;
            });
            input.setAttribute('data-index', String(i));

            row.appendChild(radio);
            row.appendChild(letter);
            row.appendChild(input);
            choiceOptionsEl.appendChild(row);
        });
    }

    function render() {
        tabs.forEach(function (tab) {
            tab.classList.toggle('active', tab.getAttribute('data-type') === state.type);
        });
        if (builder) builder.hidden = !state.type;
        renderContent();
        if (fillPanel) fillPanel.hidden = state.type !== 'FILL';
        if (judgePanel) judgePanel.hidden = state.type !== 'JUDGE';
        if (choicePanel) choicePanel.hidden = state.type !== 'CHOICE';
        if (blankKey) blankKey.style.display = (state.type === 'FILL' || state.type === 'CHOICE') ? '' : 'none';
        if (sortInput) sortInput.value = state.sort;
        if (state.type === 'FILL') {
            renderAnswers();
        }
        if (state.type === 'CHOICE') {
            renderChoiceAnswers();
            renderChoiceOptions();
        }
        var radios = document.querySelectorAll('input[name="judge"]');
        radios.forEach(function (r) {
            r.checked = r.value === state.judge;
        });
    }

    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            var type = tab.getAttribute('data-type');
            if (type !== state.type) {
                state.type = type;
                state.answers = [];
                if (type === 'CHOICE' && state.options.length === 0) {
                    state.options = [[], [], [], []];
                    state.correct = 0;
                }
            }
            render();
        });
    });

    if (keypad) {
        keypad.addEventListener('click', function (e) {
            var btn = e.target.closest ? e.target.closest('.vk') : null;
            if (!btn) return;
            var key = btn.getAttribute('data-key');
            if (key === 'back') {
                state.content = state.content.slice(0, -1);
            } else if (key === 'clear') {
                state.content = '';
            } else if (key === 'space') {
                return;
            } else {
                if (state.content.length >= 120) return;
                state.content += key;
            }
            render();
        });
    }

    var radios = document.querySelectorAll('input[name="judge"]');
    radios.forEach(function (r) {
        r.addEventListener('change', function () {
            state.judge = this.value;
        });
    });

    function variantsOf(value) {
        var s = String(value || '').replace(/\s+/g, '');
        var out = [];
        if (/^\d+$/.test(s)) {
            var n = parseInt(s, 10);
            out.push(String(n + 1), String(Math.max(0, n - 1)), String(n + 2),
                     String(n + 10), String(Math.max(0, n - 2)), String(n * 2));
            return out;
        }
        var m = s.match(/\d+/);
        if (m) {
            var v = parseInt(m[0], 10);
            [1, -1, 2, 10].forEach(function (d) {
                out.push(s.replace(/\d+/, String(Math.max(0, v + d))));
            });
        }
        return out;
    }

    function generateOptions() {
        readChoiceAnswers();
        var blanks = blankCount();
        if (blanks < 1) {
            showModal('😯', '先在题干里放一个 □ 吧～');
            return;
        }
        for (var a = 0; a < blanks; a++) {
            if (!state.answers[a] || !state.answers[a].trim()) {
                showModal('😯', '第 ' + (a + 1) + ' 空还没填正确答案～');
                return;
            }
        }

        var correct = state.answers.slice(0, blanks).map(function (v) { return v.trim(); });
        var opts = [correct.slice()];
        var seen = {};
        seen[correct.join('|')] = true;

        function tryAdd(arr) {
            var key = arr.join('|');
            if (seen[key]) return false;
            seen[key] = true;
            opts.push(arr.slice());
            return opts.length >= 4;
        }

        var done = false;
        for (var i = 0; i < blanks && !done; i++) {
            var variants = variantsOf(correct[i]);
            for (var k = 0; k < variants.length && !done; k++) {
                if (variants[k] === correct[i]) continue;
                var t = correct.slice();
                t[i] = variants[k];
                done = tryAdd(t);
            }
        }

        if (!done && blanks >= 2) {
            for (var x = 0; x < blanks && !done; x++) {
                for (var y = x + 1; y < blanks && !done; y++) {
                    var vx = variantsOf(correct[x]);
                    var vy = variantsOf(correct[y]);
                    for (var p = 0; p < vx.length && !done; p++) {
                        for (var q = 0; q < vy.length && !done; q++) {
                            var tt = correct.slice();
                            tt[x] = vx[p];
                            tt[y] = vy[q];
                            done = tryAdd(tt);
                        }
                    }
                }
            }
        }

        var step = 1;
        while (opts.length < 4 && step < 12) {
            var fallback = correct.map(function (v) {
                return /^\d+$/.test(v) ? String(parseInt(v, 10) + step) : v + '+' + step;
            });
            tryAdd(fallback);
            step++;
        }

        // 打乱顺序（保持正确项引用）
        for (var s = opts.length - 1; s > 0; s--) {
            var r = Math.floor(Math.random() * (s + 1));
            var tmp = opts[s];
            opts[s] = opts[r];
            opts[r] = tmp;
        }

        state.options = opts;
        state.correct = opts.findIndex(function (o) { return o.join('|') === correct.join('|'); });
        renderChoiceOptions();
    }
    if (btnGenOptions) {
        btnGenOptions.addEventListener('click', generateOptions);
    }

    if (sortInput) {
        sortInput.addEventListener('input', function () {
            state.sort = Number(sortInput.value) || 0;
        });
    }

    function showMsg(text, ok) {
        if (!saveMsg) return;
        saveMsg.hidden = false;
        saveMsg.className = 'var-msg ' + (ok ? 'ok' : 'bad');
        saveMsg.textContent = text;
    }

    if (btnSave) {
        btnSave.addEventListener('click', function () {
            if (!state.type) {
                showModal('😯', '请先选择题目类型～');
                return;
            }
            if (!state.content) {
                showModal('😯', '先写出题干吧～');
                return;
            }
            if (state.type === 'FILL') {
                if (blankCount() < 1) {
                    showModal('😯', '填空题至少要放一个 □ 哦～');
                    return;
                }
                for (var i = 0; i < state.answers.length; i++) {
                    if (!state.answers[i] || !state.answers[i].trim()) {
                        showModal('😯', '第 ' + (i + 1) + ' 空还没填正确答案～');
                        return;
                    }
                }
            } else if (state.type === 'JUDGE') {
                if (!state.judge) {
                    showModal('😯', '请选择这道题是正确还是错误～');
                    return;
                }
            } else if (state.type === 'CHOICE') {
                var blanks = blankCount();
                if (blanks < 1) {
                    showModal('😯', '先在题干里放一个 □ 吧～');
                    return;
                }
                readChoiceAnswers();
                for (var a = 0; a < blanks; a++) {
                    if (!state.answers[a] || !state.answers[a].trim()) {
                        showModal('😯', '第 ' + (a + 1) + ' 空还没填正确答案～');
                        return;
                    }
                }
                if (state.options.length < 2) {
                    showModal('😯', '选择题至少要 2 个选项哦～');
                    return;
                }
                for (var oi = 0; oi < state.options.length; oi++) {
                    var opt = state.options[oi];
                    if (!Array.isArray(opt) || opt.length !== blanks) {
                        showModal('😯', '第 ' + (oi + 1) + ' 个选项要填满 ' + blanks + ' 个空哦～');
                        return;
                    }
                    for (var oj = 0; oj < opt.length; oj++) {
                        if (!opt[oj] || !opt[oj].trim()) {
                            showModal('😯', '第 ' + (oi + 1) + ' 个选项还有空没填～');
                            return;
                        }
                    }
                }
                if (state.correct < 0 || state.correct >= state.options.length) {
                    showModal('😯', '请选中一个正确选项～');
                    return;
                }
            }

            var oldLabel = btnSave.textContent;
            btnSave.disabled = true;
            btnSave.classList.add('loading');
            btnSave.textContent = '保存中…';
            var payload = {
                id: state.id,
                type: state.type,
                content: state.content,
                answers: state.answers,
                judgeAnswer: state.judge,
                options: state.type === 'CHOICE' ? state.options : null,
                correctOption: state.type === 'CHOICE' ? state.correct : null,
                sortOrder: state.sort
            };
            fetch(ctx + 'variation/admin/save', {
                method: 'POST',
                credentials: 'same-origin',
                headers: headers(),
                body: JSON.stringify(payload)
            }).then(function (r) { return r.json(); })
              .then(function (data) {
                  if (data && data.success) {
                      showModal('✅', data.message || '已保存');
                      setTimeout(function () {
                          window.location.href = ctx + 'variation/admin/list';
                      }, 900);
                  } else {
                      showModal('😯', (data && data.message) || '保存失败');
                      btnSave.disabled = false;
                      btnSave.classList.remove('loading');
                      btnSave.textContent = oldLabel;
                  }
              })
              .catch(function () {
                  showModal('😯', '保存失败，检查网络后再试～');
                  btnSave.disabled = false;
                  btnSave.classList.remove('loading');
                  btnSave.textContent = oldLabel;
              });
        });
    }

    render();
})();
