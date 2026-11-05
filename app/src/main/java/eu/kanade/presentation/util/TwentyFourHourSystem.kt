package eu.kanade.presentation.util

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.domain.ui.UiPreferences
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.format.DateTimeFormatter

@Composable
internal fun rememberIs24HourSystem(): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var is24Hour by remember(context) { mutableStateOf(DateFormat.is24HourFormat(context)) }
    DisposableEffect(lifecycle, context) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                is24Hour = DateFormat.is24HourFormat(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return is24Hour
}

@Composable
internal fun rememberTimestampFormatter(includeDate: Boolean = false): DateTimeFormatter {
    val preferences = remember { Injekt.get<UiPreferences>() }
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = rememberIs24HourSystem()
    val timeFormat by preferences.timeFormat.collectAsState()
    if (!includeDate) {
        return remember(timeFormat, locale, is24Hour) { UiPreferences.timeFormat(timeFormat, locale, is24Hour) }
    }
    val dateFormat by preferences.dateFormat.collectAsState()
    return remember(dateFormat, timeFormat, locale, is24Hour) {
        UiPreferences.dateTimeFormat(dateFormat, timeFormat, locale, is24Hour)
    }
}
