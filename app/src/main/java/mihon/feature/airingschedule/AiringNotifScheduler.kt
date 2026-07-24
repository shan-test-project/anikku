package mihon.feature.airingschedule

import android.content.Context

/**
 * Thin facade over [AiringNotifWorker] that mirrors the [BellNotifyState]
 * semantics exposed by the Airing Schedule UI.
 *
 * Call from the ViewModel when the user taps the bell icon.
 *
 * @param title              Anime display title (used as a unique work key).
 * @param episode            The next upcoming episode number.
 * @param coverUrl           Remote cover-art URL.
 * @param airingAtSeconds    Unix timestamp (seconds) when the episode airs.
 */
object AiringNotifScheduler {

    /**
     * Schedule (or re-schedule) a one-off notification for a single upcoming episode.
     * Replaces any existing notification for this anime+episode pair.
     */
    fun scheduleOnce(
        context: Context,
        title: String,
        episode: Int,
        coverUrl: String?,
        airingAtSeconds: Long,
    ) {
        AiringNotifWorker.schedule(
            context = context,
            title = title,
            episode = episode,
            coverUrl = coverUrl,
            airingAtEpochSeconds = airingAtSeconds,
        )
    }

    /**
     * Cancel the pending one-off notification for a single episode.
     */
    fun cancelOnce(context: Context, title: String, episode: Int) {
        AiringNotifWorker.cancel(context, title, episode)
    }

    /**
     * Toggle helper: if a notification is not currently scheduled, schedule it;
     * if it is, cancel it.
     *
     * Returns `true` if a notification was just scheduled, `false` if it was cancelled.
     *
     * Note: WorkManager does not expose a direct "is work pending?" query.
     * The ViewModel should track [BellNotifyState] itself and call the appropriate
     * [scheduleOnce] / [cancelOnce] method directly.
     */
    fun scheduleForSeries(
        context: Context,
        title: String,
        episodes: List<Pair<Int, Long>>, // episode number → airingAt seconds
        coverUrl: String?,
    ) {
        episodes.forEach { (epNum, airingAt) ->
            AiringNotifWorker.schedule(
                context = context,
                title = title,
                episode = epNum,
                coverUrl = coverUrl,
                airingAtEpochSeconds = airingAt,
            )
        }
    }

    /**
     * Cancel all pending notifications for this anime (used when the user turns off SERIES mode).
     */
    fun cancelForSeries(context: Context, title: String, episodes: List<Int>) {
        episodes.forEach { epNum ->
            AiringNotifWorker.cancel(context, title, epNum)
        }
    }
}
