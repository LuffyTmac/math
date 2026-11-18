/**
 * 轻量富文本编辑器（无外部依赖）
 * 基于 contenteditable + document.execCommand
 *
 * 用法：
 *   <div id="editor" class="rich-editor" contenteditable="true"></div>
 *   <input type="hidden" name="content" id="contentHidden" th:value="${...}"/>
 *   <script th:src="@{/js/rich-editor.js}"></script>
 *   <script>initRichEditor('editor', 'contentHidden');</script>
 *
 *   表单提交前会自动把 editor 的 innerHTML 同步到 hidden input。
 *
 * 工具栏按钮：
 *   .re-btn[data-cmd]    -> execCommand 命令按钮
 *   .re-btn.re-btn-img   -> 图片上传按钮（内嵌 file input）
 *
 * CSRF：
 *   上传图片走 POST /upload/image，需要 CSRF token。
 *   请在页面 <head> 中加：
 *     <meta name="_csrf" th:content="${_csrf.token}"/>
 *     <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
 *   initRichEditor 会自动读取并附加到 fetch 请求头。
 */
function initRichEditor(editorId, hiddenInputId) {
    var editor = document.getElementById(editorId);
    var hidden = document.getElementById(hiddenInputId);
    if (!editor || !hidden) return;

    // 初始内容：从 hidden input 回填到编辑区
    if (hidden.value) {
        editor.innerHTML = hidden.value;
    }

    var toolbar = editor.parentElement.querySelector('.re-toolbar');

    // 普通命令按钮
    if (toolbar) {
        toolbar.querySelectorAll('.re-btn:not(.re-btn-img)').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                var cmd = btn.getAttribute('data-cmd');
                var value = btn.getAttribute('data-value') || null;
                editor.focus();
                try {
                    document.execCommand(cmd, false, value);
                } catch (err) {
                    // 忽略不支持的命令
                }
                syncToHidden();
            });
        });

        // 图片上传按钮
        var imgBtn = toolbar.querySelector('.re-btn-img');
        if (imgBtn) {
            var fileInput = imgBtn.querySelector('input[type=file]');
            if (fileInput) {
                fileInput.addEventListener('change', function () {
                    var file = fileInput.files[0];
                    if (!file) return;
                    uploadImage(file, editor, imgBtn);
                    // 清空 value 让同一文件可再次选择
                    fileInput.value = '';
                });
            }
        }
    }

    // 编辑时实时同步到 hidden input
    editor.addEventListener('input', syncToHidden);
    editor.addEventListener('blur', syncToHidden);

    function syncToHidden() {
        hidden.value = editor.innerHTML;
    }

    // 粘贴时清除样式（只保留纯文本 / 基础格式）
    editor.addEventListener('paste', function (e) {
        // 如果粘贴的是图片，直接插入（部分浏览器支持）
        var items = (e.clipboardData || window.clipboardData).items;
        if (items) {
            for (var i = 0; i < items.length; i++) {
                if (items[i].type && items[i].type.indexOf('image') === 0) {
                    var imgFile = items[i].getAsFile();
                    if (imgFile) {
                        e.preventDefault();
                        uploadImage(imgFile, editor, null);
                        return;
                    }
                }
            }
        }
        e.preventDefault();
        var text = (e.clipboardData || window.clipboardData).getData('text/plain');
        document.execCommand('insertText', false, text);
    });
}

/**
 * 上传图片到 /upload/image 并插入到编辑器
 * @param file       File 对象
 * @param editor     contenteditable 元素
 * @param btn        触发按钮（用于显示 loading / 错误提示，可为 null）
 */
function uploadImage(file, editor, btn) {
    if (!file) return;
    // 简单的前端校验
    if (file.size > 5 * 1024 * 1024) {
        alert('图片不能超过 5MB 哦～');
        return;
    }
    var allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'];
    var ok = false;
    for (var i = 0; i < allowed.length; i++) {
        if (allowed[i] === file.type) { ok = true; break; }
    }
    if (!ok) {
        alert('仅支持 jpg/png/gif/webp/bmp 格式～');
        return;
    }

    var formData = new FormData();
    formData.append('file', file);

    // 读取 CSRF token（页面 meta 标签）
    var csrfToken = '';
    var csrfHeader = 'X-CSRF-TOKEN';
    var metaToken = document.querySelector('meta[name="_csrf"]');
    var metaHeader = document.querySelector('meta[name="_csrf_header"]');
    if (metaToken) csrfToken = metaToken.getAttribute('content') || '';
    if (metaHeader) csrfHeader = metaHeader.getAttribute('content') || 'X-CSRF-TOKEN';

    var headers = {};
    if (csrfToken) headers[csrfHeader] = csrfToken;

    // 占位：在光标处插入一个 loading 提示
    editor.focus();
    var placeholderId = 'img-loading-' + Date.now();
    var placeholder = document.createElement('span');
    placeholder.id = placeholderId;
    placeholder.textContent = '⏳ 图片上传中...';
    placeholder.style.cssText = 'color:#fa8c16;background:#fff7e6;padding:2px 8px;border-radius:4px;font-size:13px;';
    document.execCommand('insertHTML', false, '<span id="' + placeholderId + '" style="color:#fa8c16;background:#fff7e6;padding:2px 8px;border-radius:4px;font-size:13px;">⏳ 图片上传中...</span>');

    fetch('/upload/image', {
        method: 'POST',
        headers: headers,
        body: formData
    }).then(function (resp) {
        return resp.json();
    }).then(function (data) {
        // 移除占位
        var ph = document.getElementById(placeholderId);
        if (ph) ph.parentNode.removeChild(ph);
        if (data && data.errno === 0 && data.url) {
            editor.focus();
            document.execCommand('insertImage', false, data.url);
            // 触发 input 事件同步到 hidden
            editor.dispatchEvent(new Event('input', { bubbles: true }));
        } else {
            alert('图片上传失败：' + (data && data.message ? data.message : '未知错误'));
        }
    }).catch(function (err) {
        var ph = document.getElementById(placeholderId);
        if (ph) ph.parentNode.removeChild(ph);
        alert('图片上传失败：' + err.message);
    });
}
