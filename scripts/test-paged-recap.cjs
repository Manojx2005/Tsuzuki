// Run from the repository root: node scripts/test-paged-recap.cjs [chromium executable]
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const assets = 'app/src/main/assets/novel-reader';
const builder = fs.readFileSync(
    'app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/text/webview/NovelWebViewDocumentBuilder.kt',
    'utf8',
);
const cssStart = builder.indexOf('tsundoku-chapter {');
const cssEnd = builder.indexOf('/* The infinite-scroll chapter-boundary marker', cssStart);
assert.ok(cssStart >= 0 && cssEnd > cssStart, 'Missing reader CSS');
const css = builder.slice(cssStart, cssEnd).replaceAll('$TSUNDOKU_CHAPTERS_CONTAINER_ID', 'reader-pages');
const pagingTokens = {
    TSUNDOKU_OBJECT_NAME: 'fixtureReader',
    PAGED_BODY_CLASS: 'tsundoku-paged',
    PAGE_EVENT: 'page-change',
    PROGRESS_EVENT: 'reading-progress',
    CHAPTERS_CONTAINER_ID: 'reader-pages',
    PAGED_ENABLED: 'true',
    PAGED_DRAG_COMMIT_FRACTION: '0.2',
    PAGED_EDGE_COMMIT_FRACTION: '0.2',
};
const paging = fs.readFileSync(`${assets}/paged-reader.js`, 'utf8')
    .replace(/__([A-Z_]+)__/g, (match, name) => pagingTokens[name] ?? match);
const recap = fs.readFileSync(`${assets}/chapter-recap-card.js`, 'utf8').replace('__RECAP__', JSON.stringify({
    chapterId: '42',
    title: 'Previously',
    dismiss: 'Dismiss',
    paragraphs: [
        'The archivist waited for the second bell at the gate. '.repeat(9),
        'A lantern beside the door cast shadows across the floor. '.repeat(9),
        'The ferry left the harbor just after the morning fog cleared. '.repeat(9),
    ],
}));

const story = Array.from({ length: 40 }, (_, i) => (
    `<p data-story="${i + 1}">Paragraph ${i + 1}. The tide began to rise while the crew arranged the crates on the deck. `
    + 'There was another hour of daylight, and the captain checked the weather again. '
    + 'By the time they reached the old wharf, everyone was looking for the lantern at the end of the quay.</p>'
)).join('\n');

function fixture(withRecap, width, height) {
    const chapter = width === 390 ? `<main>${story}</main>` : `<tsundoku-chapter>${story}</tsundoku-chapter>`;
    return `<!doctype html><meta charset="utf-8">
        <style>body {margin:16px;font:18px/1.55 Georgia,serif} p{margin:0 0 12px} ${css}</style>
        ${chapter}<script>
        const dismissCalls = [];
        let saved;
        window.Android = {
            dismissNovelRecap(id) { dismissCalls.push(id); },
            onScrollProgress(ratio) { saved = ratio; },
        };
        ${withRecap ? recap : ''}
        addEventListener('load', () => {
            ${paging}
            const snapshot = () => {
                const range = document.createRange();
                return Array.from(document.querySelectorAll('[data-story]'), p => {
                    range.selectNodeContents(p);
                    return Array.from(range.getClientRects())
                        .filter(r => r.right > 16 && r.left < innerWidth - 16 && r.bottom > 16 && r.top < innerHeight - 16)
                        .map(r => [p.dataset.story, r.top, r.width]);
                }).flat();
            };
            const p = window.__tdPaged;
            const card = document.getElementById('tsuzuki-novel-recap');
            window.__tdPagedRestoreRatio(0, 1);
            const start = { page: p.pageIndex, progress: saved, lines: snapshot() };
            const totalPages = p.pageCount;
            const recapFragments = card?.getClientRects().length ?? 0;
            window.__tdPagedRestoreRatio(0.6, 2);
            const resumed = snapshot();
            fixtureReader.actions.nextPage();
            const turnProgress = saved;
            window.__tdPagedRestoreRatio(turnProgress, 3);
            const turned = snapshot();
            window.__tdPagedRestoreRatio(1, 4);
            const end = { lines: snapshot(), onLastPage: p.pageIndex === p.pageCount - 1 };
            window.__tdPagedRestoreRatio(turnProgress, 5);
            card?.querySelector('button').click();
            const dismissed = { page: p.pageIndex, lines: snapshot(), progress: saved };
            parent.postMessage({
                withRecap: ${withRecap}, width: ${width}, height: ${height}, start, totalPages, recapFragments,
                resumed, turned, turnProgress, dismissed, dismissCalls, end,
            }, '*');
        });</script>`;
}

const sizes = [[390, 650], [753, 1205], [650, 390]];
const frames = sizes.flatMap(([width, height]) => [false, true].map(withRecap => ({
    width, height, source: fixture(withRecap, width, height),
})));
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'tsuzuki-paged-recap-'));
try {
    const harness = path.join(tmp, 'fixture.html');
    const frameJson = JSON.stringify(frames).replaceAll('</script', '<\\/script');
    fs.writeFileSync(harness, `<!doctype html><meta charset="utf-8"><pre id="result"></pre><script>
        const results = [];
        addEventListener('message', event => {
            results.push(event.data);
            document.getElementById('result').textContent = JSON.stringify(results);
        });
        for (const { width, height, source } of ${frameJson}) {
            const frame = document.createElement('iframe');
            frame.width = width;
            frame.height = height;
            frame.srcdoc = source;
            document.body.appendChild(frame);
        }
    </script>`);

    const browser = process.argv[2] ?? (process.platform === 'win32'
        ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : 'chromium');
    const dom = execFileSync(browser, [
        '--headless', '--disable-gpu', '--disable-background-networking', '--no-first-run', '--no-default-browser-check',
        `--user-data-dir=${path.join(tmp, 'profile')}`, '--virtual-time-budget=3000', '--dump-dom', pathToFileURL(harness).href,
    ], { encoding: 'utf8', timeout: 30000, stdio: ['ignore', 'pipe', 'pipe'] });
    const results = JSON.parse(dom.match(/<pre id="result">([^<]+)<\/pre>/)?.[1] ?? '[]');
    assert.equal(results.length, frames.length, 'All fixtures must finish');
    for (const [width, height] of sizes) {
        const plain = results.find(result => result.width === width && !result.withRecap);
        const withCard = results.find(result => result.width === width && result.withRecap);
        assert.ok(plain.resumed.length, 'Story did not render');
        assert.ok(withCard.recapFragments > 0, 'Recap did not render');
        assert.deepEqual(withCard.resumed, plain.resumed, 'Recap moved the resumed story lines');
        assert.deepEqual(withCard.start, { page: 0, progress: 0, lines: [] });
        assert.equal(withCard.totalPages - plain.totalPages, withCard.recapFragments);
        assert.deepEqual(withCard.turned, plain.turned, 'A page turn must land on the same story lines');
        assert.equal(withCard.turnProgress, plain.turnProgress, 'Page turns must save story progress');
        assert.deepEqual(withCard.dismissCalls, ['42']);
        assert.deepEqual(withCard.dismissed, plain.dismissed, 'Dismiss must keep the current story page and progress');
        assert.deepEqual(withCard.end, plain.end, 'Recap must preserve the chapter end');
        console.log(`Paged recap passed at ${width} × ${height}. Recap page count = ${withCard.recapFragments}`);
    }
} finally {
    fs.rmSync(tmp, { recursive: true, force: true, maxRetries: 5, retryDelay: 100 });
}
