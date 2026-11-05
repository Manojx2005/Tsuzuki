package eu.kanade.presentation.tsuzuki.reader

import android.content.res.Resources
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import android.graphics.Typeface as AndroidTypeface
import android.graphics.fonts.Font as AndroidFont
import android.graphics.fonts.FontFamily as AndroidFontFamily

// Exception to "no direct fontFamily": quotes use the reader's Literata face, which MaterialTheme.typography doesn't expose.
private val literata = FontFamily(Font(R.font.lnr_literata))

private object ReadingFont {
    @Volatile
    private var cached: FontFamily? = null

    fun get(resources: Resources): FontFamily = cached ?: synchronized(this) {
        cached ?: run {
            val paired = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { pairedFamily(resources) }.getOrNull()
            } else {
                null
            }
            (paired ?: literata).also { cached = it }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun pairedFamily(resources: Resources): FontFamily {
        val latin = AndroidFont.Builder(resources, R.font.lnr_literata).build()
        val japanese = AndroidFont.Builder(resources, R.font.lnr_jp_notoserif).build()
        return FontFamily(
            Typeface(
                AndroidTypeface.CustomFallbackBuilder(AndroidFontFamily.Builder(latin).build())
                    .addCustomFallback(AndroidFontFamily.Builder(japanese).build())
                    .setSystemFallback("serif")
                    .build(),
            ),
        )
    }
}

/** Reading excerpts use the bundled reader face while surrounding controls keep the app typography. */
@Composable
internal fun TsuzukiReadingTypography(content: @Composable () -> Unit) {
    val resources = LocalContext.current.resources
    val readingFamily = remember(resources) { ReadingFont.get(resources) }
    val typography = MaterialTheme.typography
    MaterialTheme(
        typography = typography.copy(
            bodyLarge = typography.bodyLarge.copy(
                fontFamily = readingFamily,
                fontSize = 15.sp,
                lineHeight = 22.5.sp,
                letterSpacing = 0.sp,
            ),
        ),
        content = content,
    )
}
