package mihon.feature.airingschedule

data class AiringScheduleEntry(
    val airingAt: Long,
    val episode: Int,
    val mediaId: Int,
    val titleUserPreferred: String = "",
    val titleEnglish: String? = null,
    val titleRomaji: String? = null,
    val titleNative: String? = null,
    val coverImageUrl: String = "",
    val isAdult: Boolean = false,
    val totalEpisodes: Int? = null,
    val format: String? = null,
    val averageScore: Int? = null,
) {
    fun displayTitle(language: SchedulePreferences.TitleLanguage): String = when (language) {
        SchedulePreferences.TitleLanguage.ENGLISH -> titleEnglish ?: titleRomaji ?: titleUserPreferred
        SchedulePreferences.TitleLanguage.ROMAJI  -> titleRomaji ?: titleUserPreferred
        SchedulePreferences.TitleLanguage.NATIVE  -> titleNative ?: titleUserPreferred
        SchedulePreferences.TitleLanguage.USER_PREFERRED -> titleUserPreferred
    }

    fun hasAired(): Boolean = System.currentTimeMillis() / 1000L >= airingAt
}

class AiringScheduleRepository {
    suspend fun getWeeklySchedule(
        start: Long,
        end: Long,
        includeAdult: Boolean,
    ): List<AiringScheduleEntry> = emptyList()
}
