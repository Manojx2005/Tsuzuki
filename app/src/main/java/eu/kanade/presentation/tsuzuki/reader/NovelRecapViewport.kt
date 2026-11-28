package eu.kanade.presentation.tsuzuki.reader

import android.graphics.Rect
import android.view.View
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.text.textview.ChapterTextBlock
import logcat.LogPriority
import org.json.JSONArray
import org.json.JSONException
import tachiyomi.core.common.util.system.logcat

/** Sample the rendered text after restoration, rather than treating a scroll ratio as a text ratio. */
internal fun reportNativeNovelRecapViewport(
    activity: ReaderActivity,
    chapterId: Long?,
    block: ChapterTextBlock,
    viewport: View,
) {
    if (chapterId == null || !activity.viewModel.needsNovelRecapViewport(chapterId)) return
    val text = block.fullText ?: return
    val visible = Rect()
    if (!viewport.getGlobalVisibleRect(visible)) return
    val location = IntArray(2)
    for ((index, chunk) in block.chunkViews.withIndex()) {
        val layout = chunk.layout ?: continue
        chunk.getLocationOnScreen(location)
        val textTop = location[1] + chunk.totalPaddingTop
        if (textTop + layout.height <= visible.top || textTop >= visible.bottom) continue
        val line = layout.getLineForVertical((visible.top - textTop).coerceAtLeast(0))
        val offset = (block.chunkStarts.getOrNull(index) ?: 0) + layout.getLineStart(line)
        // A partially visible paragraph is incomplete, even when its opening lines are above the viewport.
        val paragraphs = block.paragraphBoundsByNewline().takeWhile { (_, end) -> end <= offset }
            .takeLast(16).filter { (start, end) -> end - start <= 8192 }
            .map { (start, end) -> text.substring(start, end) }
        activity.viewModel.noteNovelRecapViewport(chapterId, paragraphs)
        return
    }
    // No reliable visible text (for example a short chapter's footer): use the previous chapter.
    activity.viewModel.noteNovelRecapViewport(chapterId, emptyList())
}

internal fun reportWebNovelRecapViewport(
    activity: ReaderActivity,
    chapterId: Long?,
    isCurrent: () -> Boolean,
    evaluate: (String, (String) -> Unit) -> Unit,
) {
    if (chapterId == null || activity.isFinishing || !isCurrent() ||
        !activity.viewModel.needsNovelRecapViewport(chapterId)
    ) {
        return
    }
    val script = NOVEL_RECAP_VIEWPORT_SCRIPT.replace("__CHAPTER_ID__", chapterId.toString())
    evaluate(script) { raw ->
        if (activity.isFinishing || !isCurrent() ||
            !activity.viewModel.needsNovelRecapViewport(chapterId)
        ) {
            return@evaluate
        }
        val paragraphs = try {
            val data = JSONArray(raw)
            (0 until data.length()).map { data.getString(it) }
        } catch (error: JSONException) {
            activity.logcat(LogPriority.WARN) {
                "Could not read the restored recap viewport (${error.javaClass.simpleName})"
            }
            emptyList()
        }
        activity.viewModel.noteNovelRecapViewport(chapterId, paragraphs)
    }
}

/** Both-axis fragment intersections work for scrolling and multi-column paged chapters. */
internal val NOVEL_RECAP_VIEWPORT_SCRIPT = """
    (function() {
        var root = document.querySelector('tsundoku-chapter[data-chapter-id="__CHAPTER_ID__"]');
        if (!root) return [];
        var selector = 'p,li,blockquote,pre,div';
        var blocks = Array.from(root.querySelectorAll(selector)).filter(function(el) {
            if (el.querySelector(selector) || el.closest('nav,aside,header,footer,[hidden],[data-chapter-end-card],#tsuzuki-novel-recap')) return false;
            var style = getComputedStyle(el);
            return style.display !== 'none' && style.visibility !== 'hidden' && el.innerText.trim().length > 0;
        });
        var samples = [];
        blocks.forEach(function(el) {
            // Half an em: a real line fragment (even one CJK character) is wider; a trailing space that overhangs
            // the hidden prior column into the visible one (about 3 CSS px on the tablet) is not.
            var minExtent = Math.max(1, 0.5 * (parseFloat(getComputedStyle(el).fontSize) || 16));
            if (!el.matches('pre[data-tsundoku-plain-text]')) {
                samples.push({text: el.innerText, rects: Array.from(el.getClientRects()), min: minExtent});
                return;
            }
            // Plain novels are one pre element. Measure its newline paragraphs as DOM ranges.
            var nodes = [], walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT), node, offset = 0;
            while ((node = walker.nextNode())) {
                nodes.push({node: node, start: offset, end: offset + node.length});
                offset += node.length;
            }
            offset = 0;
            el.textContent.split('\n').forEach(function(line) {
                var trimmed = line.trim(), start = offset + line.search(/\S/), end = start + trimmed.length;
                offset += line.length + 1;
                if (!trimmed) return;
                var range = document.createRange(), began = false;
                for (var n = 0; n < nodes.length; n++) {
                    var part = nodes[n];
                    if (!began && start >= part.start && start <= part.end) {
                        range.setStart(part.node, start - part.start);
                        began = true;
                    }
                    if (began && end <= part.end) {
                        range.setEnd(part.node, end - part.start);
                        samples.push({text: trimmed, rects: Array.from(range.getClientRects()), min: minExtent});
                        break;
                    }
                }
            });
        });
        var height = window.innerHeight || document.documentElement.clientHeight;
        var width = window.innerWidth || document.documentElement.clientWidth;
        var clipLeft = 0, clipRight = width, clipTop = 0, clipBottom = height;
        var bodyStyle = getComputedStyle(document.body), bodyRect = document.body.getBoundingClientRect();
        // Paged mode clips its columns at the body's reading margins, not at the window edge.
        if (bodyStyle.overflowX === 'hidden' || bodyStyle.overflowX === 'clip') {
            clipLeft = Math.max(clipLeft, bodyRect.left);
            clipRight = Math.min(clipRight, bodyRect.right);
        }
        if (bodyStyle.overflowY === 'hidden' || bodyStyle.overflowY === 'clip') {
            clipTop = Math.max(clipTop, bodyRect.top);
            clipBottom = Math.min(clipBottom, bodyRect.bottom);
        }
        // RECAP-VIEWPORT-F: a paragraph is on the settled page only when a fragment overlaps the clip by at least
        // half an em both ways, so whitespace slivers from the hidden prior column don't count as visible text.
        function onPage(r, min) {
            var w = Math.min(r.right, clipRight) - Math.max(r.left, clipLeft);
            var h = Math.min(r.bottom, clipBottom) - Math.max(r.top, clipTop);
            return w >= min && h >= min;
        }
        var before = [];
        for (var i = 0; i < samples.length; i++) {
            var rects = samples[i].rects, min = samples[i].min;
            if (!rects.length) continue;
            if (rects.some(function(r) { return onPage(r, min); })) return before.slice(-16);
            // Stop before content on a future page, even if a preceding page has no prose.
            if (rects.some(function(r) { return (r.right > clipLeft && r.left < clipRight && r.top >= clipBottom) || r.left >= clipRight; })) return before.slice(-16);
            var text = samples[i].text;
            if (text.length <= 8192) before.push(text);
        }
        // The recap can resume onto a footer page with every prose paragraph behind it.
        return before.slice(-16);
    })();
""".trimIndent()
