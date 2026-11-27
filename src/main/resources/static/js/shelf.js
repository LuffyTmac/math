        (function(){
            const initEl = document.getElementById('shelfPage');
            function initNum(name, fallback) {
                const v = initEl ? parseInt(initEl.getAttribute('data-' + name), 10) : NaN;
                return isNaN(v) ? fallback : v;
            }

            const state = {
                leftLayers: initNum('left-layers', 3),
                leftBooks: initNum('left-books', 10),
                rightLayers: initNum('right-layers', 3),
                rightBooks: initNum('right-books', 5)
            };

            const LIMITS = {
                layers: { min: 1, max: 10 },
                books: { min: 1, max: Infinity }
            };

            const leftLayersVal = document.getElementById('leftLayersVal');
            const leftBooksVal = document.getElementById('leftBooksVal');
            const rightLayersVal = document.getElementById('rightLayersVal');
            const rightBooksVal = document.getElementById('rightBooksVal');

            const leftShelfVisual = document.getElementById('leftShelfVisual');
            const rightShelfVisual = document.getElementById('rightShelfVisual');
            const leftBadge = document.getElementById('leftBadge');
            const rightBadge = document.getElementById('rightBadge');

            const submitBtn = document.getElementById('submitBtn');
            const toast = document.getElementById('toast');
            let toastTimer = null;

            function clamp(value, min, max) {
                return Math.min(max, Math.max(min, value));
            }

            function updateButtons() {
                document.getElementById('leftLayersMinus').disabled = state.leftLayers <= LIMITS.layers.min;
                document.getElementById('leftLayersPlus').disabled = state.leftLayers >= LIMITS.layers.max;
                document.getElementById('rightLayersMinus').disabled = state.rightLayers <= LIMITS.layers.min;
                document.getElementById('rightLayersPlus').disabled = state.rightLayers >= LIMITS.layers.max;

                document.getElementById('leftBooksMinus').disabled = state.leftBooks <= LIMITS.books.min;
                document.getElementById('leftBooksPlus').disabled = false;
                document.getElementById('rightBooksMinus').disabled = state.rightBooks <= LIMITS.books.min;
                document.getElementById('rightBooksPlus').disabled = false;
            }

            function updateValueDisplays() {
                leftLayersVal.textContent = state.leftLayers;
                leftBooksVal.textContent = state.leftBooks;
                rightLayersVal.textContent = state.rightLayers;
                rightBooksVal.textContent = state.rightBooks;
            }

            function generateBooksHTML(bookCount) {
                if (bookCount <= 0) return '<span class="empty-message">📭 这一层没有书</span>';
                let booksHtml = '';
                for (let i = 0; i < bookCount; i++) {
                    booksHtml += `<div class="book" title="一本书"></div>`;
                }
                return booksHtml;
            }

            function renderShelf(container, layers, booksPerLayer, badgeEl) {
                container.innerHTML = '';
                for (let layer = 1; layer <= layers; layer++) {
                    const rowDiv = document.createElement('div');
                    rowDiv.className = 'shelf-row';

                    // 层数标记
                    const layerBadge = document.createElement('span');
                    layerBadge.className = 'layer-badge';
                    layerBadge.textContent = `第${layer}层`;

                    const booksContainer = document.createElement('div');
                    booksContainer.className = 'books-container';
                    booksContainer.innerHTML = generateBooksHTML(booksPerLayer);

                    rowDiv.appendChild(layerBadge);
                    rowDiv.appendChild(booksContainer);
                    container.appendChild(rowDiv);
                }
                badgeEl.textContent = `${layers}层 · 每层${booksPerLayer}本`;
            }

            function renderAll() {
                updateValueDisplays();
                updateButtons();
                renderShelf(leftShelfVisual, state.leftLayers, state.leftBooks, leftBadge);
                renderShelf(rightShelfVisual, state.rightLayers, state.rightBooks, rightBadge);
            }

            function setupStepper(buttonId, targetKey, delta) {
                const btn = document.getElementById(buttonId);
                btn.addEventListener('click', function() {
                    const isLayers = targetKey.includes('Layers');
                    const limit = isLayers ? LIMITS.layers : LIMITS.books;
                    const newVal = clamp(state[targetKey] + delta, limit.min, limit.max);
                    if (newVal !== state[targetKey]) {
                        state[targetKey] = newVal;
                        renderAll();
                    }
                });
            }

            function showToast(message, ok) {
                toast.textContent = message;
                toast.classList.toggle('ok', ok !== false);
                toast.classList.toggle('bad', ok === false);
                toast.classList.add('show');
                if (toastTimer) clearTimeout(toastTimer);
                toastTimer = setTimeout(() => {
                    toast.classList.remove('show');
                }, 2400);
            }

            function csrfToken() {
                const meta = document.querySelector('meta[name="_csrf"]');
                return meta ? meta.getAttribute('content') : '';
            }

            function csrfHeader() {
                const meta = document.querySelector('meta[name="_csrf_header"]');
                return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
            }

            // 提交按钮：把左右书架配置保存到后台
            submitBtn.addEventListener('click', function () {
                if (submitBtn.disabled) return;
                const oldLabel = submitBtn.textContent;
                submitBtn.disabled = true;
                submitBtn.classList.add('loading');
                submitBtn.textContent = '提交中…';

                const headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
                headers[csrfHeader()] = csrfToken();

                let ctx = document.body.getAttribute('data-ctx') || '/';
                if (ctx.charAt(ctx.length - 1) !== '/') ctx += '/';

                const body = 'leftLayers=' + state.leftLayers
                    + '&leftBooks=' + state.leftBooks
                    + '&rightLayers=' + state.rightLayers
                    + '&rightBooks=' + state.rightBooks;

                fetch(ctx + 'learn/bookshelf/submit', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: headers,
                    body: body
                }).then(function (resp) {
                    return resp.json();
                }).then(function (data) {
                    if (data && data.success) {
                        showToast(data.message || '✅ 配置已提交！', true);
                        const countEl = document.getElementById('shelfCount');
                        if (countEl) countEl.textContent = String((parseInt(countEl.textContent, 10) || 0) + 1);
                        const latestEl = document.getElementById('shelfLatest');
                        if (latestEl) {
                            let detail = document.getElementById('shelfLatestDetail');
                            if (!detail) {
                                detail = document.createElement('span');
                                detail.id = 'shelfLatestDetail';
                                latestEl.appendChild(detail);
                            }
                            detail.textContent = ' · 最近一次：左 ' + state.leftLayers + '层×' + state.leftBooks
                                + '本，右 ' + state.rightLayers + '层×' + state.rightBooks + '本';
                        }
                    } else {
                        showToast((data && data.message) || '提交失败，再试一次吧～', false);
                    }
                }).catch(function () {
                    showToast('提交没成功，检查一下网络再试试～', false);
                }).finally(function () {
                    submitBtn.disabled = false;
                    submitBtn.classList.remove('loading');
                    submitBtn.textContent = oldLabel;
                });
            });

            setupStepper('leftLayersMinus', 'leftLayers', -1);
            setupStepper('leftLayersPlus', 'leftLayers', 1);
            setupStepper('leftBooksMinus', 'leftBooks', -1);
            setupStepper('leftBooksPlus', 'leftBooks', 1);
            setupStepper('rightLayersMinus', 'rightLayers', -1);
            setupStepper('rightLayersPlus', 'rightLayers', 1);
            setupStepper('rightBooksMinus', 'rightBooks', -1);
            setupStepper('rightBooksPlus', 'rightBooks', 1);

            renderAll();
        })();