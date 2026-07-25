package mihon.feature.airingschedule

data class AiringScheduleEntry(
    val airingAt: Long,
    val episode: Int,
    val mediaId: Int,
)

class AiringScheduleRepository {
    suspend fun getWeeklySchedule(
        start: Long,
        end: Long,
        includeAdult: Boolean,
    ): List<AiringScheduleEntry> = emptyList()
}
