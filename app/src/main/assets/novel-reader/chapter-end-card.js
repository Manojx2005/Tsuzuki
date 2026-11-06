(function () {
    var data = __CARD__;
    var existing = document.getElementById('__BTN_CONTAINER_ID__');
    if (existing) existing.remove();

    function element(parent, type, text, style) {
        var node = document.createElement(type);
        node.textContent = text || '';
        node.style.cssText = style;
        parent.appendChild(node);
        return node;
    }

    var body = getComputedStyle(document.body);
    var page = body.backgroundColor;
    var pageInk = body.color;
    var ink = data.ink || pageInk;
    var dot = Boolean(data.ink);
    var container = element(document.body, 'div', '',
        'padding:24px 16px calc(24px + var(__SAFE_BOTTOM_VAR__,0px));' +
        'text-align:start;text-indent:0;line-height:1.35;letter-spacing:normal;' +
        'font:14px/1.35 ' + (dot ? 'monospace' : 'sans-serif') + ';color:' + ink);
    container.id = '__BTN_CONTAINER_ID__';
    container.dataset.chapterId = data.chapterId;

    element(container, 'div', data.end, 'margin-bottom:20px;text-align:center;color:' + pageInk);
    var card = element(container, 'div', '',
        'display:grid;gap:8px;border-radius:24px;padding:20px;background:' +
        (data.tile || 'color-mix(in srgb,' + pageInk + ' 8%,' + page + ')'));
    element(card, 'div', data.upNext, 'font-weight:bold;');
    if (data.number) {
        element(card, 'div', data.number, 'font-size:28px;' + (dot ? '' : 'font-family:serif;font-style:italic;'));
    }
    element(card, 'div', data.title, 'font-size:16px;font-weight:bold;overflow-wrap:anywhere;');
    var downloaded = element(card, 'div', data.downloaded, '');
    downloaded.dataset.downloaded = '';
    downloaded.hidden = true;

    var common = 'cursor:pointer;border-radius:28px;font:inherit;font-weight:bold;' +
        'padding:8px;white-space:normal;text-transform:none;';
    var next = element(card, 'button', data.next + ' →',
        common + 'min-height:56px;margin:8px 0;border:0;background:' +
        (data.button || pageInk) + ';color:' + (data.buttonInk || page));
    next.onclick = function () { Android.loadNextChapter(); };

    var actions = element(card, 'div', '', 'display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;');
    var secondary = common + 'min-height:48px;background:transparent;color:' + ink +
        ';border:1px solid color-mix(in srgb,' + ink + ' 36%,transparent);';
    element(actions, 'button', data.markRead, secondary).onclick = function () {
        Android.markChapterEndRead(data.chapterId);
    };
    element(actions, 'button', data.contents, secondary).onclick = function () {
        Android.openChapterEndContents(data.chapterId);
    };
})();
