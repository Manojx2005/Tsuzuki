package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmapOrNull
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** AP7: tapping the details cover shows it full size with Save and Share, as book details do. */
@Composable
internal fun AnimeCoverDialog(source: AnimeSource?, title: String, url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val request = rememberAnimeImage(source, url)
    fun save(share: Boolean) {
        scope.launch {
            try {
                val uri = saveAnimeCover(context, request, title, temp = share) ?: error("No cover")
                if (share) context.startActivity(uri.toShareIntent(context)) else context.toast(MR.strings.cover_saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AnimeLog.w("Could not save the anime cover", e)
                context.toast(if (share) MR.strings.error_sharing_cover else MR.strings.error_saving_cover)
            }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, stringResource(MR.strings.action_close), tint = Color.White)
                }
                Box(Modifier.weight(1f))
                IconButton(onClick = { save(share = true) }) {
                    Icon(Icons.Outlined.Share, stringResource(MR.strings.action_share), tint = Color.White)
                }
                IconButton(onClick = { save(share = false) }) {
                    Icon(Icons.Outlined.Save, stringResource(MR.strings.action_save), tint = Color.White)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = request,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private suspend fun saveAnimeCover(context: Context, request: ImageRequest, title: String, temp: Boolean): Uri? =
    withIOContext {
        val full = request.newBuilder().size(Size.ORIGINAL).build()
        val bitmap = context.imageLoader.execute(full).image?.asDrawable(context.resources)?.toBitmapOrNull()
            ?: return@withIOContext null
        Injekt.get<ImageSaver>().save(
            Image.Cover(
                bitmap = bitmap,
                name = title,
                location = if (temp) Location.Cache else Location.Pictures.create(),
            ),
        )
    }
