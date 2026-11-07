package eu.kanade.presentation.tsuzuki.selection

import androidx.compose.runtime.Composable
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
internal fun tsuzukiScreenExtras(configure: MutableCreationExtras.() -> Unit): CreationExtras {
    val owner = LocalViewModelStoreOwner.current as? HasDefaultViewModelProviderFactory
    return MutableCreationExtras(owner?.defaultViewModelCreationExtras ?: CreationExtras.Empty).apply(configure)
}

/** Keep transient bulk selections small enough to share the activity's saved-state transaction. */
internal fun Collection<Long>.savedSelection(): LongArray =
    if (size <= MAX_SAVED_SELECTION) toLongArray() else longArrayOf()

internal const val MAX_SAVED_SELECTION = 8192
