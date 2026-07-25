package mihon.feature.airingschedule

class UploadDelayTracker {
    private val observations = mutableMapOf<String, MutableList<Long>>()

    fun recordObservation(sourceId: String, delayMinutes: Long) {
        observations.getOrPut(sourceId) { mutableListOf() }.add(delayMinutes)
    }

    fun getAverageDelay(sourceId: String): Long? {
        val obs = observations[sourceId] ?: return null
        return if (obs.isEmpty()) null else obs.average().toLong()
    }
}
