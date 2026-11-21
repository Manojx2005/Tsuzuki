package eu.kanade.tachiyomi.tsuzuki.anime.player

import tachiyomi.core.common.preference.PreferenceStore

/** Subtitle text colours: a fixed list of pills rather than a colour picker (PARITY §9.4, no board for a picker). */
internal enum class AnimeSubtitleColor(val mpv: String) {
    WHITE("#FFFFFFFF"),
    YELLOW("#FFFFFF00"),
    CYAN("#FF00FFFF"),
    GREEN("#FF00FF00"),
}

internal enum class AnimeSubtitleBorder(val mpv: String?) {
    BLACK("#FF000000"),
    GREY("#FF808080"),
    NONE(null),
}

/**
 * How subtitles look (AY3), mapped onto mpv's `sub-*` options the way Aniyomi's `setupSubtitlesOptions` does.
 * Plain-text subtitles (SRT, VTT) always take it; styled ASS tracks keep their own look unless [overrideAss].
 */
internal data class AnimeSubtitleStyle(
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val bold: Boolean = false,
    val color: AnimeSubtitleColor = AnimeSubtitleColor.WHITE,
    val border: AnimeSubtitleBorder = AnimeSubtitleBorder.BLACK,
    val borderSize: Int = DEFAULT_BORDER_SIZE,
    val background: Boolean = false,
    val position: Int = DEFAULT_POSITION,
    val overrideAss: Boolean = false,
) {
    /** Clamps every number into its range, so stored or restored values can't reach mpv out of range. */
    fun bounded() = copy(
        fontSize = fontSize.coerceIn(FONT_SIZES),
        borderSize = borderSize.coerceIn(BORDER_SIZES),
        position = position.coerceIn(POSITIONS),
    )

    companion object {
        val FONT_SIZES = 30..80
        val BORDER_SIZES = 0..5
        val POSITIONS = 50..100
        const val DEFAULT_FONT_SIZE = 55
        const val DEFAULT_BORDER_SIZE = 3
        const val DEFAULT_POSITION = 95
        const val FONT_SIZE_STEP = 5
        const val POSITION_STEP = 5
    }
}

/** mpv option values for [style], in the order they're applied. */
internal fun subtitleStyleOptions(style: AnimeSubtitleStyle): List<Pair<String, String>> {
    val s = style.bounded()
    val border = s.border.mpv
    return listOf(
        "sub-font-size" to s.fontSize.toString(),
        "sub-bold" to if (s.bold) "yes" else "no",
        "sub-color" to s.color.mpv,
        "sub-border-color" to (border ?: "#00000000"),
        "sub-border-size" to if (border == null) "0" else s.borderSize.toString(),
        // A translucent box behind the text instead of an outline (mpv 0.38+: background-box).
        "sub-border-style" to if (s.background) "background-box" else "outline-and-shadow",
        "sub-back-color" to if (s.background) "#99000000" else "#00000000",
        "sub-pos" to s.position.toString(),
        // Off keeps mpv's default ("scale": ASS keeps its look, only the size setting applies).
        "sub-ass-override" to if (s.overrideAss) "force" else "scale",
    )
}

/** The changed options only, so a settings change doesn't reset what didn't change. */
internal fun changedSubtitleStyleOptions(
    previous: AnimeSubtitleStyle?,
    next: AnimeSubtitleStyle,
): List<Pair<String, String>> {
    val before = previous?.let(::subtitleStyleOptions)?.toMap().orEmpty()
    return subtitleStyleOptions(next).filter { (name, value) -> before[name] != value }
}

internal class AnimeSubtitleStylePreferences(store: PreferenceStore) {
    private val fontSize = store.getInt("tsuzuki_anime_player_sub_font_size", AnimeSubtitleStyle.DEFAULT_FONT_SIZE)
    private val bold = store.getBoolean("tsuzuki_anime_player_sub_bold", false)
    private val color = store.getString("tsuzuki_anime_player_sub_color", AnimeSubtitleColor.WHITE.name)
    private val border = store.getString("tsuzuki_anime_player_sub_border", AnimeSubtitleBorder.BLACK.name)
    private val borderSize =
        store.getInt("tsuzuki_anime_player_sub_border_size", AnimeSubtitleStyle.DEFAULT_BORDER_SIZE)
    private val background = store.getBoolean("tsuzuki_anime_player_sub_background", false)
    private val position = store.getInt("tsuzuki_anime_player_sub_position", AnimeSubtitleStyle.DEFAULT_POSITION)
    private val overrideAss = store.getBoolean("tsuzuki_anime_player_sub_override_ass", false)

    fun get() = AnimeSubtitleStyle(
        fontSize = fontSize.get(),
        bold = bold.get(),
        color = AnimeSubtitleColor.entries.firstOrNull { it.name == color.get() } ?: AnimeSubtitleColor.WHITE,
        border = AnimeSubtitleBorder.entries.firstOrNull { it.name == border.get() } ?: AnimeSubtitleBorder.BLACK,
        borderSize = borderSize.get(),
        background = background.get(),
        position = position.get(),
        overrideAss = overrideAss.get(),
    ).bounded()

    fun set(style: AnimeSubtitleStyle) {
        val s = style.bounded()
        fontSize.set(s.fontSize)
        bold.set(s.bold)
        color.set(s.color.name)
        border.set(s.border.name)
        borderSize.set(s.borderSize)
        background.set(s.background)
        position.set(s.position)
        overrideAss.set(s.overrideAss)
    }
}
