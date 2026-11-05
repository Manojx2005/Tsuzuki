package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class TimeFormat(val titleRes: StringResource) {
    FOLLOW_LANGUAGE(MR.strings.tsz_time_follow_language),
    FOLLOW_SYSTEM(MR.strings.tsz_time_follow_system),
    HOURS_24(MR.strings.tsz_time_24_hours),
    HOURS_12(MR.strings.tsz_time_12_hours),
    ;

    internal fun uses24HourClock(is24HourSystem: Boolean): Boolean? = when (this) {
        FOLLOW_LANGUAGE -> null
        FOLLOW_SYSTEM -> is24HourSystem
        HOURS_24 -> true
        HOURS_12 -> false
    }
}
