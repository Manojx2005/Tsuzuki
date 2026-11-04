package eu.kanade.presentation.tsuzuki.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/** Keeps library state across navigation, while cancelling collectors for a replaced type. */
class TsuzukiLibraryViewModelStore : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private var currentType: Any? = null

    fun forType(type: Any): ViewModelStoreOwner {
        if (currentType != type) {
            viewModelStore.clear()
            currentType = type
        }
        return this
    }

    override fun onCleared() {
        viewModelStore.clear()
    }
}

@Composable
fun rememberTsuzukiLibraryViewModelStoreOwner(type: Any): ViewModelStoreOwner {
    val holder = viewModel<TsuzukiLibraryViewModelStore>()
    return remember(holder, type) { holder.forType(type) }
}
