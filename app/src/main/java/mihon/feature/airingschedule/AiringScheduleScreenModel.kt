package mihon.feature.airingschedule

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class AiringScheduleScreenModel : StateScreenModel<AiringScheduleScreenModel.State>(State()) {

    private val repository = AiringScheduleRepository()
    private val schedulePrefs: SchedulePreferences = Injekt.get()
    private val uploadDelayTracker: UploadDelayTracker = Injekt.get()

    private var allEntries: List<AiringScheduleEntry> = emptyList()

    init {
        loadSchedule()
        observePreferences()
    }

    private fun observePreferences() {
        screenModelScope.launch {
            combine(
                schedulePrefs.showOnlyFavoriteSources().changes(),
                schedulePrefs.filterBySourceAvailability().changes(),
                schedulePrefs.favoriteSourceIds().changes(),
                schedulePrefs.showAdultContent().changes(),
                schedulePrefs.titleLanguage().changes(),
            ) { _, _, _, _, _ -> Unit }.collectLatest {
                applyFilters()
            }
        }
    }

    fun loadSchedule() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            try {
                val zone = ZoneId.systemDefault()
                val now = ZonedDateTime.now(zone)
                val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate().atStartOfDay(zone)
                val weekEnd = weekStart.plusDays(7).minusSeconds(1)

                val includeAdult = schedulePrefs.showAdultContent().get()
                val entries = repository.getWeeklySchedule(
                    weekStart.toEpochSecond(),
                    weekEnd.toEpochSecond(),
                    includeAdult = includeAdult,
                )

                allEntries = entries

                val delays = if (schedulePrefs.uploadDelayEnabled().get()) {
                    uploadDelayTracker.getDelays()
                } else {
                    emptyMap()
                }

                applyFilters(
                    entries = allEntries,
                    delays = delays,
                    weekStart = weekStart.toLocalDate(),
                    weekEnd = weekEnd.toLocalDate(),
                )
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun applyFilters(
        entries: List<AiringScheduleEntry> = allEntries,
        delays: Map<String, Long> = if (schedulePrefs.uploadDelayEnabled().get()) uploadDelayTracker.getDelays() else emptyMap(),
        weekStart: LocalDate? = mutableState.value.weekStartDate,
        weekEnd: LocalDate? = mutableState.value.weekEndDate,
    ) {
        val filterByAvailability = schedulePrefs.filterBySourceAvailability().get()
        val favoriteIds = schedulePrefs.favoriteSourceIds().get()
        val titleLang = schedulePrefs.titleLanguage().get()
        val zone = ZoneId.systemDefault()

        val filtered = entries.filter { entry ->
            // filterByAvailability: only show episodes that have already aired,
            // giving sources time to upload the episode.
            // showOnlyFavoriteSources: requires per-entry source membership data which
            // is not available from AniList; treated as a UI pass-through until
            // library/source integration provides that linkage.
            !filterByAvailability || entry.hasAired()
        }

        val grouped = filtered.groupBy { entry ->
            val airTime = if (delays.isNotEmpty() && favoriteIds.isNotEmpty()) {
                val avgDelay = delays.entries
                    .filter { it.key in favoriteIds }
                    .map { it.value }
                    .let { if (it.isEmpty()) 0L else it.average().toLong() }
                entry.airingAt + (avgDelay * 60)
            } else {
                entry.airingAt
            }
            ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(airTime),
                zone,
            ).dayOfWeek
        }

        mutableState.update {
            it.copy(
                isLoading = false,
                scheduleByDay = grouped,
                weekStartDate = weekStart,
                weekEndDate = weekEnd,
                titleLanguage = titleLang,
                sourceDelays = delays,
                favoriteSourceIds = favoriteIds,
            )
        }
    }

    fun selectDay(day: DayOfWeek) {
        mutableState.update { it.copy(selectedDay = day) }
    }

    fun clearLearnedDelays() {
        uploadDelayTracker.clearAllDelays()
        applyFilters(delays = emptyMap())
    }

    data class State(
        val isLoading: Boolean = true,
        val scheduleByDay: Map<DayOfWeek, List<AiringScheduleEntry>> = emptyMap(),
        val selectedDay: DayOfWeek = ZonedDateTime.now().dayOfWeek,
        val weekStartDate: LocalDate? = null,
        val weekEndDate: LocalDate? = null,
        val error: String? = null,
        val titleLanguage: SchedulePreferences.TitleLanguage = SchedulePreferences.TitleLanguage.USER_PREFERRED,
        val sourceDelays: Map<String, Long> = emptyMap(),
        val favoriteSourceIds: Set<String> = emptySet(),
    )
}
