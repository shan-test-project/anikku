package mihon.feature.airingschedule

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class SchedulePreferences(
    private val preferenceStore: PreferenceStore,
) {
    enum class TitleLanguage { USER_PREFERRED, ENGLISH, ROMAJI, NATIVE }
    enum class UploadDelayInterval { THIRTY_MIN, ONE_HOUR, TWO_HOURS, SIX_HOURS, TWELVE_HOURS, NEVER }

    fun favoriteSourceIds() = preferenceStore.getStringSet(
        "schedule_favorite_source_ids",
        emptySet(),
    )

    fun showOnlyFavoriteSources() = preferenceStore.getBoolean(
        "schedule_show_only_favorite_sources",
        false,
    )

    fun filterBySourceAvailability() = preferenceStore.getBoolean(
        "schedule_filter_by_source_availability",
        false,
    )

    fun titleLanguage() = preferenceStore.getEnum(
        "schedule_title_language",
        TitleLanguage.USER_PREFERRED,
    )

    fun showAdultContent() = preferenceStore.getBoolean(
        "schedule_show_adult_content",
        false,
    )

    fun uploadDelayEnabled() = preferenceStore.getBoolean(
        "schedule_upload_delay_enabled",
        false,
    )

    fun uploadDelayRefreshInterval() = preferenceStore.getEnum(
        "schedule_upload_delay_interval",
        UploadDelayInterval.ONE_HOUR,
    )

    fun sourceUploadDelays() = preferenceStore.getString(
        "schedule_source_upload_delays",
        "{}",
    )

    fun lastDelayCheckTime() = preferenceStore.getLong(
        "schedule_last_delay_check_time",
        0L,
    )

    fun scheduledAlarmKeys() = preferenceStore.getStringSet(
        "schedule_scheduled_alarm_keys",
        emptySet(),
    )
}
