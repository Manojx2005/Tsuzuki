(function () {
    var data = __RECAP__;

    function showRecap() {
        var page = getComputedStyle(document.body);
        var ink = data.ink || page.color;
        var card = document.createElement('aside');
        card.id = 'tsuzuki-novel-recap';
        card.dataset.chapterId = data.chapterId;
        card.setAttribute('aria-label', data.title);
        card.style.cssText = 'display:block;box-sizing:border-box;break-inside:avoid;' +
            'margin:12px 0;padding:4px 16px 16px;border-radius:24px;' +
            'text-align:start;text-indent:0;letter-spacing:normal;user-select:none;color:' + ink + ';' +
            'font:14px/1.5 ' + (data.ink ? 'monospace' : 'sans-serif') + ';background:' +
            (data.tile || 'color-mix(in srgb,' + page.color + ' 8%,' + page.backgroundColor + ')') + ';' +
            (data.border ? 'border:1px solid ' + data.border + ';' : '');
        // Reader-wide * overrides must not replace this UI card's theme typography and ink.
        card.style.setProperty('font-family', data.ink ? 'monospace' : 'sans-serif', 'important');
        card.style.setProperty('font-size', '14px', 'important');
        card.style.setProperty('color', ink, 'important');

        var header = document.createElement('div');
        header.style.cssText = 'display:flex;justify-content:space-between;align-items:center;gap:8px;';
        var title = document.createElement('strong');
        title.textContent = data.title;
        header.appendChild(title);
        var dismiss = document.createElement('button');
        dismiss.type = 'button';
        dismiss.textContent = data.dismiss;
        dismiss.style.cssText = 'min-height:48px;background:transparent;border:0;' +
            'padding:0 8px;cursor:pointer;font:inherit;color:' + (data.accent || ink);
        dismiss.style.setProperty('color', data.accent || ink, 'important');
        dismiss.onclick = function () {
            card.remove();
            Android.dismissNovelRecap(data.chapterId);
            if (window.__tdPagedRepaginate) {
                window.__tdPagedRepaginate();
            } else {
                window.dispatchEvent(new Event('scroll'));
            }
        };
        header.appendChild(dismiss);
        card.appendChild(header);

        data.paragraphs.forEach(function (paragraph) {
            var text = document.createElement('div');
            text.textContent = paragraph;
            text.style.cssText = 'margin-top:8px;overflow-wrap:anywhere;';
            card.appendChild(text);
        });
        document.body.insertBefore(card, document.body.firstChild);
        window.__tszNovelRecapHeight = function () {
            return card.isConnected ? card.offsetHeight + 24 : 0;
        };
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', showRecap, { once: true });
    } else {
        showRecap();
    }
})();
