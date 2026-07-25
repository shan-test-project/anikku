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

    companion object {
        /** Returns the expected availability epoch-second given the official air time and learned delay. */
        fun adjustedAirTime(airingAt: Long, delayMinutes: Long): Long = airingAt + (delayMinutes * 60L)
    }
}
