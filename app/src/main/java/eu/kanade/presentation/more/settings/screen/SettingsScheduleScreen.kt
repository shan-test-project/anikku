package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.presentation.more.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import mihon.feature.airingschedule.SchedulePreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.ank.AMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsScheduleScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_schedule

    @Composable
    override fun getPreferences(): List<Preference> {
        val schedulePreferences = remember { Injekt.get<SchedulePreferences>() }
        val getExtensionsByType = remember { Injekt.get<GetExtensionsByType>() }
        val extensionsState by getExtensionsByType.subscribe().collectAsState(initial = null)

        val installedSourceOptions = remember(extensionsState) {
            extensionsState?.installed
                ?.flatMap { ext -> ext.sources.map { src -> src.id.toString() to "${ext.name} \u203a ${src.name}" } }
                ?.distinctBy { it.first }
                ?.sortedBy { it.second }
                ?.toMap()
                ?.toImmutableMap()
                ?: emptyMap<String, String>().toImmutableMap()
        }

        val titleLanguageOptions = mapOf(
            SchedulePreferences.TitleLanguage.USER_PREFERRED to stringResource(AMR.strings.pref_schedule_lang_user_preferred),
            SchedulePreferences.TitleLanguage.ENGLISH to stringResource(AMR.strings.pref_schedule_lang_english),
            SchedulePreferences.TitleLanguage.ROMAJI to stringResource(AMR.strings.pref_schedule_lang_romaji),
            SchedulePreferences.TitleLanguage.NATIVE to stringResource(AMR.strings.pref_schedule_lang_native),
        ).toImmutableMap()

        val intervalOptions = mapOf(
            SchedulePreferences.UploadDelayInterval.THIRTY_MIN to stringResource(AMR.strings.pref_schedule_interval_30min),
            SchedulePreferences.UploadDelayInterval.ONE_HOUR to stringResource(AMR.strings.pref_schedule_interval_1h),
            SchedulePreferences.UploadDelayInterval.TWO_HOURS to stringResource(AMR.strings.pref_schedule_interval_2h),
            SchedulePreferences.UploadDelayInterval.SIX_HOURS to stringResource(AMR.strings.pref_schedule_interval_6h),
            SchedulePreferences.UploadDelayInterval.TWELVE_HOURS to stringResource(AMR.strings.pref_schedule_interval_12h),
            SchedulePreferences.UploadDelayInterval.NEVER to stringResource(AMR.strings.pref_schedule_interval_never),
        ).toImmutableMap()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_schedule_sources_title),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.MultiSelectListPreference(
                        pref = schedulePreferences.favoriteSourceIds(),
                        title = stringResource(MR.strings.pref_schedule_favorite_sources),
                        subtitle = stringResource(MR.strings.pref_schedule_favorite_sources_summary),
                        entries = installedSourceOptions,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.showOnlyFavoriteSources(),
                        title = stringResource(MR.strings.pref_schedule_show_only_favorites),
                        subtitle = stringResource(MR.strings.pref_schedule_show_only_favorites_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.filterBySourceAvailability(),
                        title = stringResource(AMR.strings.pref_schedule_filter_availability),
                        subtitle = stringResource(AMR.strings.pref_schedule_filter_availability_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.autoAddViaPinnedSources(),
                        title = stringResource(AMR.strings.pref_schedule_auto_add_pinned),
                        subtitle = stringResource(AMR.strings.pref_schedule_auto_add_pinned_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(AMR.strings.pref_schedule_upload_delay_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.uploadDelayEnabled(),
                        title = stringResource(AMR.strings.pref_schedule_upload_delay_enabled),
                        subtitle = stringResource(AMR.strings.pref_schedule_upload_delay_enabled_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = schedulePreferences.uploadDelayRefreshInterval(),
                        title = stringResource(AMR.strings.pref_schedule_refresh_interval),
                        subtitle = stringResource(AMR.strings.pref_schedule_refresh_interval_summary),
                        entries = intervalOptions,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(AMR.strings.pref_schedule_display_group),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = schedulePreferences.titleLanguage(),
                        title = stringResource(AMR.strings.pref_schedule_title_language),
                        subtitle = stringResource(AMR.strings.pref_schedule_title_language_summary),
                        entries = titleLanguageOptions,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.showAdultContent(),
                        title = stringResource(AMR.strings.pref_schedule_show_adult),
                        subtitle = stringResource(AMR.strings.pref_schedule_show_adult_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_schedule_about_title),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(AMR.strings.pref_schedule_about_info),
                    ),
                ),
            ),
        )
    }
}
