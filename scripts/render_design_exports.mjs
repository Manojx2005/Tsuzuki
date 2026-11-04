#!/usr/bin/env node
// Render only downloaded, self-contained Claude Design exports. No signed-in browser is used.
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { gunzipSync } from 'node:zlib';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const args = process.argv.slice(2);
const option = (name, fallback) => {
  const index = args.indexOf(name);
  return index < 0 ? fallback : args[index + 1];
};
const input = path.resolve(option('--input', 'docs/design/boards/exports'));
const output = path.resolve(option('--output', 'build/reports/design-render-check'));
const browserPath = option('--browser', 'C:/Program Files/Google/Chrome/Application/chrome.exe');
const packageRoot = option('--packages', 'C:/Users/Manoj/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules');
const scale = Number(option('--scale', '2'));
const fontCache = path.resolve(option('--font-cache', 'build/reports/design-font-cache'));
const fetchPublicFonts = args.includes('--fetch-public-fonts');
const require = createRequire(path.join(packageRoot, '__design_renderer.cjs'));
const { chromium } = require('playwright');
const slug = value => value.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').replace(/[^A-Za-z0-9]+/g, '-').replace(/^-|-$/g, '').toLowerCase();
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const unescape = text => text.replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
const embedded = (html, name) => {
  const match = html.match(new RegExp(`<script type="__bundler/${name}">([\\s\\S]*?)<\\/script>`));
  if (!match) throw new Error(`Missing embedded ${name}`);
  return JSON.parse(match[1]);
};
const attrs = html => Object.fromEntries([...html.matchAll(/([\w-]+)="([^"]*)"/g)].map(match => [match[1], unescape(match[2])]));
const canonical = {
  'What do you read': 'OnboardingContent',
  'Library with manga': 'LibraryMixed',
  'Browse manga sources': 'BrowseManga',
  'Manhwa details': 'MangaDetails',
  'Manga reader paged': 'MangaPaged',
  'Manga reader': 'MangaReader',
  'Manga reader settings': 'MangaReaderSettings',
  'Tablet manga spread': 'TabletMangaSpread',
  'Dot theme, manga': 'DotManga',
};
// Canvas order, verified against each exported gallery's headings. Archives keep title slugs.
const pageNames = {
  '01': ['Brand'],
  '02': ['MotionSpec', 'MotionOpen', 'MotionReader', 'MotionStack', 'MotionTheme', 'MotionSheet', 'MotionSplash'],
  '03': ['Main', 'LibraryList', 'LibraryDisplay', 'LibrarySearch', 'LibraryEmpty', 'Updates', 'History', 'More', 'DotMain'],
  '04': ['Browse', 'Extensions', 'ExtensionDetail', 'ExtensionRepos', 'Catalogue', 'CatalogueFilters', 'GlobalSearch', 'WebView', 'SourceHealth', 'Migration', 'MigrationCompare', 'DotBrowse'],
  '05': ['NovelDetails', 'ChapterFilter', 'DownloadSheet', 'AddCategory', 'EditNovel', 'Tracking', 'TrackerSearch', 'MergeSources', 'Duplicate', 'DotNovel'],
  '06': ['Reader', 'ReaderImmersive', 'ReaderPaged', 'ReaderSettings', 'ReaderTOC', 'ReaderFind', 'ChapterEnd', 'Dictionary', 'Translate', 'TTS', 'ListenMini', 'Notes', 'ReplaceRules', 'DotReader'],
  '07': ['Downloads', 'Stats', 'Categories', 'Settings', 'SettingsReader', 'SettingsAppearance', 'Backup', 'AppLock', 'About', 'Onboarding', 'Widgets', 'DotSystem'],
  '08': ['CustomSource', 'ElementSelector', 'ChapterSearch', 'DuplicateFinder', 'SourcePriority', 'ImportEpub', 'MassImport', 'ExportEpub', 'LibraryQuery', 'Upcoming', 'FontManager', 'ReaderBars', 'Quotes', 'SettingsTranslation', 'ReaderEngine', 'UpdateSchedule', 'DataTools', 'TrackerAccounts', 'DotTools'],
  '09': ['OnboardingContent', 'LibraryMixed', 'BrowseManga', 'MangaDetails', 'MangaPaged', 'MangaReader', 'MangaReaderSettings', 'TabletMangaSpread', 'DotManga'],
  '10': ['AnimeHome', 'AnimeLibrary', 'AnimeDetails', 'BrowseAnime', 'PlayerSettings', 'Player', 'PlayerSheets', 'AnimeTracking', 'DotAnime'],
  '11': ['TabletLibrary', 'TabletBrowse', 'TabletReader', 'TabletTTS', 'TabletUpdates', 'TabletNovel', 'TabletReaderPanel', 'TabletSettings', 'DotTablet'],
  '12': ['TabletPortraitLibrary', 'TabletPortraitReader', 'Foldable', 'DotPortrait'],
  '13': ['DesktopLibrary', 'Desktop', 'DesktopReader', 'DotDesktop'],
  '14': ['Cover', 'Nav', 'Bar', 'Rail', 'Icon', 'Logo'],
};

await fs.mkdir(output, { recursive: true });
await fs.mkdir(fontCache, { recursive: true });
const inputStat = await fs.stat(input);
const files = inputStat.isDirectory()
  ? (await fs.readdir(input)).filter(file => file.endsWith('.html')).sort().map(file => path.join(input, file))
  : [input];
const browser = await chromium.launch({ executablePath: browserPath, headless: true });
const previous = args.includes('--resume')
  ? await fs.readFile(path.join(output, 'manifest.json'), 'utf8').then(JSON.parse).catch(() => null)
  : null;
const manifest = {
  capturedAt: new Date().toISOString(),
  scale,
  input: path.relative(output, input).replaceAll('\\', '/'),
  publicFontCache: path.relative(process.cwd(), fontCache).replaceAll('\\', '/'),
  pages: previous?.scale === scale ? previous.pages : [],
};
try {
  for (const file of files) {
    const sourceBytes = await fs.readFile(file);
    const html = sourceBytes.toString('utf8');
    const packed = embedded(html, 'manifest');
    const template = embedded(html, 'template');
    const pageSlug = path.basename(file, '.html');
    const existing = manifest.pages.find(item => item.page === pageSlug);
    const expectedNames = pageNames[pageSlug.slice(0, 2)];
    const namingMatches = !expectedNames || expectedNames.every((name, index) => existing?.boards[index]?.name === name);
    if (existing?.sourceSha256 === sha(sourceBytes) && namingMatches && !existing.boards.some(item => item.errors.length || item.blockedRequests.length)) {
      const screenshots = existing.boards.flatMap(item => [item.png, ...item.screens.map(screen => screen.png)]);
      const present = await Promise.all(screenshots.map(file => fs.access(path.join(output, file)).then(() => true, () => false)));
      if (present.every(Boolean)) {
        console.log(`${pageSlug}: already rendered (${existing.boards.length} artboards)`);
        continue;
      }
    }
    const htmlDir = path.join(output, 'html', pageSlug);
    const pngDir = path.join(output, 'png', pageSlug);
    await fs.mkdir(htmlDir, { recursive: true });
    await fs.mkdir(pngDir, { recursive: true });
    const pageResult = { source: path.relative(output, file).replaceAll('\\', '/'), sourceSha256: sha(sourceBytes), page: pageSlug, boards: [] };
    const sections = [...template.matchAll(/<section class="board">([\s\S]*?)<\/section>/g)];
    if (!sections.length) {
      // A one-artboard page is exported directly instead of inside a gallery.
      const props = JSON.parse(unescape(template.match(/data-props="([^"]+)"/)?.[1] ?? '{}'));
      const preview = props.$preview;
      if (!preview?.width || !preview?.height) throw new Error(`No artboard preview size in ${file}`);
      const title = template.match(/<title>([\s\S]*?)<\/title>/)?.[1] ?? pageSlug;
      packed.__single = { data: sourceBytes.toString('base64') };
      sections.push([null, `<h2>${title}</h2><iframe src="about:blank#__single" width="${preview.width}" height="${preview.height}"></iframe>`]);
    }
    const names = pageNames[pageSlug.slice(0, 2)];
    if (names && names.length !== sections.length) throw new Error(`Expected ${names.length} boards in ${file}; found ${sections.length}. Review order before rendering.`);
    for (let index = 0; index < sections.length; index++) {
      const section = sections[index][1];
      const boardTitle = unescape(section.match(/<h2>([\s\S]*?)<\/h2>/)?.[1] ?? `Board ${index + 1}`);
      const attributes = attrs(section.match(/<iframe\s+([^>]+)>/)?.[1] ?? '');
      const id = attributes.src?.split('#')[1];
      const item = packed[id];
      if (!item) throw new Error(`No bundled HTML for ${boardTitle}`);
      const bytes = Buffer.from(item.data, 'base64');
      const boardHtml = (item.compressed ? gunzipSync(bytes) : bytes).toString('utf8');
      const innerTitle = boardHtml.match(/<title>([\s\S]*?)<\/title>/)?.[1];
      let name = names?.[index] ?? canonical[innerTitle] ?? `${String(index + 1).padStart(2, '0')}-${slug(boardTitle)}`;
      const width = Number(attributes.width);
      const height = Number(attributes.height);
      if (!Number.isFinite(width) || !Number.isFinite(height)) throw new Error(`Invalid size for ${boardTitle}`);
      let localHtml = path.join(htmlDir, `${name}.html`);
      await fs.writeFile(localHtml, boardHtml);
      const context = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: scale });
      const blocked = new Set();
      const cachedFonts = [];
      const cachedStylesheets = [];
      await context.route(/^https?:\/\//, async route => {
        const url = route.request().url();
        const parsed = new URL(url);
        const isFont = parsed.protocol === 'https:' && parsed.hostname === 'fonts.gstatic.com' && parsed.pathname.startsWith('/s/') && parsed.pathname.endsWith('.woff2');
        const isStylesheet = parsed.protocol === 'https:' && parsed.hostname === 'fonts.googleapis.com' && parsed.pathname === '/css2';
        if (isFont || isStylesheet) {
          const contentType = isFont ? 'font/woff2' : 'text/css';
          const cachedPath = path.join(fontCache, `${sha(url)}.${isFont ? 'woff2' : 'css'}`);
          let bytes = await fs.readFile(cachedPath).catch(() => null);
          if (!bytes && fetchPublicFonts) {
            const response = await fetch(url, { redirect: 'error', headers: { 'User-Agent': route.request().headers()['user-agent'] } });
            if (response.ok) {
              bytes = Buffer.from(await response.arrayBuffer());
              if (isFont && bytes.subarray(0, 4).toString() !== 'wOF2') throw new Error(`Unexpected font response: ${url}`);
              if (isStylesheet && !bytes.toString('utf8').includes('@font-face')) throw new Error(`Unexpected font stylesheet: ${url}`);
              await fs.writeFile(cachedPath, bytes);
            }
          }
          if (bytes) {
            (isFont ? cachedFonts : cachedStylesheets).push({ url, sha256: sha(bytes), contentType, cacheFile: path.relative(process.cwd(), cachedPath).replaceAll('\\', '/') });
            return route.fulfill({ body: bytes, contentType, headers: { 'Access-Control-Allow-Origin': '*' } });
          }
        }
        blocked.add(url);
        return route.abort();
      });
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.goto(pathToFileURL(localHtml).href, { waitUntil: 'load' });
      await page.waitForFunction(() => !document.querySelector('#__bundler_loading') && !!document.querySelector('x-dc, #dc-root'), { timeout: 30000 });
      await page.evaluate(async () => { await document.fonts.ready; });
      await page.waitForTimeout(250);
      const renderedName = await page.locator('#dc-root > .sc-host').getAttribute('data-sc-name').catch(() => null);
      if (renderedName && /^[A-Za-z][A-Za-z0-9_-]*$/.test(renderedName) && name !== renderedName) {
        name = renderedName;
        const renamedHtml = path.join(htmlDir, `${name}.html`);
        await fs.rename(localHtml, renamedHtml);
        localHtml = renamedHtml;
      }
      const screenshot = path.join(pngDir, `${name}.png`);
      await page.screenshot({ path: screenshot, animations: 'disabled', fullPage: false });
      const text = await page.locator('body').innerText();
      const frames = await page.locator('iframe').evaluateAll(elements => elements.map(element => ({ title: element.title, x: element.getBoundingClientRect().x, y: element.getBoundingClientRect().y, width: element.getBoundingClientRect().width, height: element.getBoundingClientRect().height })));
      const screens = [];
      if (/^Dot/.test(name) || /DOT THEME/i.test(boardTitle)) {
        const children = await page.evaluate(() => {
          const root = document.querySelector('#dc-root > .sc-host');
          return [...root.querySelectorAll('.sc-host')].filter(element => element.parentElement.closest('.sc-host') === root).map(element => {
            const rect = element.getBoundingClientRect();
            return { name: element.dataset.scName, x: rect.x, y: rect.y, width: rect.width, height: rect.height };
          }).filter(item => item.width >= 300 && item.height >= 300);
        });
        await fs.mkdir(path.join(pngDir, 'dot'), { recursive: true });
        for (const screen of children) {
          if (!/^[A-Za-z][A-Za-z0-9_-]*$/.test(screen.name)) throw new Error(`Unsafe component name ${screen.name}`);
          const target = path.join(pngDir, 'dot', `${screen.name}.png`);
          await page.screenshot({ path: target, animations: 'disabled', clip: { x: screen.x, y: screen.y, width: screen.width, height: screen.height } });
          screens.push({ ...screen, png: path.relative(output, target).replaceAll('\\', '/') });
        }
      }
      pageResult.boards.push({ order: index + 1, id, title: boardTitle, innerTitle, name, width, height, png: path.relative(output, screenshot).replaceAll('\\', '/'), html: path.relative(output, localHtml).replaceAll('\\', '/'), text, screens, nestedFrames: frames, cachedFonts, cachedStylesheets, blockedRequests: [...blocked], errors });
      console.log(`${pageSlug}: ${name} (${width}x${height})`);
      await context.close();
    }
    manifest.pages = manifest.pages.filter(item => item.page !== pageSlug);
    manifest.pages.push(pageResult);
    manifest.pages.sort((a, b) => a.page.localeCompare(b.page));
    await fs.writeFile(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  }
} finally {
  await browser.close();
}
for (const page of manifest.pages) {
  for (const board of page.boards) {
    for (const screenshot of [board, ...board.screens]) {
      const bytes = await fs.readFile(path.join(output, screenshot.png));
      screenshot.pngSha256 = sha(bytes);
      screenshot.pngBytes = bytes.length;
    }
    for (const font of board.cachedFonts) {
      if (path.isAbsolute(font.cacheFile)) font.cacheFile = path.relative(process.cwd(), font.cacheFile).replaceAll('\\', '/');
    }
  }
}
manifest.artboardCount = manifest.pages.reduce((count, item) => count + item.boards.length, 0);
manifest.individualDotScreenCount = manifest.pages.reduce((count, item) => count + item.boards.reduce((total, board) => total + board.screens.length, 0), 0);
manifest.pngBytes = manifest.pages.reduce((count, item) => count + item.boards.reduce((total, board) => total + board.pngBytes + board.screens.reduce((bytes, screen) => bytes + screen.pngBytes, 0), 0), 0);
await fs.writeFile(path.join(output, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
console.log(`Saved ${manifest.pages.reduce((count, item) => count + item.boards.length, 0)} artboards from ${manifest.pages.length} pages to ${output}`);
