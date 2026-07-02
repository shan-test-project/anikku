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
            SchedulePreferences.TitleLanguage.USER_PREFERRED to "User Preferred (AniList default)",
            SchedulePreferences.TitleLanguage.ENGLISH to "English",
            SchedulePreferences.TitleLanguage.ROMAJI to "Romaji",
            SchedulePreferences.TitleLanguage.NATIVE to "Native",
        ).toImmutableMap()

        val intervalOptions = mapOf(
            SchedulePreferences.UploadDelayInterval.THIRTY_MIN to "Every 30 minutes",
            SchedulePreferences.UploadDelayInterval.ONE_HOUR to "Every 1 hour",
            SchedulePreferences.UploadDelayInterval.TWO_HOURS to "Every 2 hours",
            SchedulePreferences.UploadDelayInterval.SIX_HOURS to "Every 6 hours",
            SchedulePreferences.UploadDelayInterval.TWELVE_HOURS to "Every 12 hours",
            SchedulePreferences.UploadDelayInterval.NEVER to "Never (manual only)",
        ).toImmutableMap()

        return listOf(
            Preference.PreferenceGroup(
                title = "Favorite Sources",
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
                        title = "Filter by source availability",
                        subtitle = "Only show anime that your selected favorite sources are likely to carry",
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.autoAddViaPinnedSources(),
                        title = "Auto-add via pinned sources",
                        subtitle = "When tapping the bookmark button, search only in your pinned sources (from Browse) using priority order \u2014 1st pinned gets highest priority",
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Upload Delay Tracking",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.uploadDelayEnabled(),
                        title = "Auto-sync source upload time",
                        subtitle = "Learn how long after the official air time each pinned source uploads episodes, then show estimated availability time. Priority: 1st pinned source \u2192 2nd \u2192 etc.",
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = schedulePreferences.uploadDelayRefreshInterval(),
                        title = "Refresh interval",
                        subtitle = "How often to check sources for new episodes. Once a delay is learned it is cached \u2014 checking stops until the interval elapses",
                        entries = intervalOptions,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = "Display",
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.ListPreference(
                        pref = schedulePreferences.titleLanguage(),
                        title = "Preferred title language",
                        subtitle = "Language used to display anime titles in the schedule",
                        entries = titleLanguageOptions,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = schedulePreferences.showAdultContent(),
                        title = "Show 18+ anime",
                        subtitle = "Include adult-only anime in the airing schedule",
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_schedule_about_title),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.InfoPreference(
                        title = "The airing schedule is powered by AniList. Upload delay tracking monitors when episodes appear on your installed sources vs the official air time, calculates the average delay, and shows estimated upload times. Tap the play or search icon on any anime to find it in your installed sources. Tap the bookmark icon to add it to your library via search. Tap the bell to notify for the next episode; hold 1 second for every episode.",
                    ),
                ),
            ),
        )
    }
}
