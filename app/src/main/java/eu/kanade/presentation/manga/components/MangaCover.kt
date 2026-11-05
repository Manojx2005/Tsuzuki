package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import eu.kanade.presentation.tsuzuki.details.rememberTsuzukiGeneratedCover
import eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover as CoverData

enum class MangaCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    ;

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
        colorFilter: androidx.compose.ui.graphics.ColorFilter? = null,
        generatedTitle: String? = null,
    ) {
        val model = (data as? ImageRequest)?.data ?: data
        val title = generatedTitle ?: when (model) {
            is CoverData -> model.title
            is Manga -> model.title
            else -> null
        }
        val generatedCover = if (isTsuzukiStyle() && !title.isNullOrBlank()) {
            rememberTsuzukiGeneratedCover(title)
        } else {
            null
        }
        val missingUrl = when (model) {
            is CoverData -> model.url.isNullOrBlank()
            is Manga -> model.thumbnailUrl.isNullOrBlank()
            is String -> model.isBlank()
            else -> model == null
        }
        AsyncImage(
            model = data,
            placeholder = if (missingUrl &&
                generatedCover != null
            ) {
                generatedCover
            } else {
                ColorPainter(CoverPlaceholderColor)
            },
            error = generatedCover ?: rememberResourceBitmapPainter(id = R.drawable.cover_error),
            fallback = generatedCover ?: rememberResourceBitmapPainter(id = R.drawable.cover_default),
            contentDescription = contentDescription,
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentScale = ContentScale.Crop,
            // Tsuzuki Dot theme: covers are greyscale everywhere (red is the only colour).
            colorFilter = colorFilter ?: eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter(),
        )
    }
}

private val CoverPlaceholderColor = Color(0x1F888888)
