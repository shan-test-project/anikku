package mihon.feature.airingschedule

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * One-shot WorkManager worker that fires the "Episode just aired" notification
 * at the moment an episode's air-time is reached.
 *
 * Schedule one per episode via [AiringNotifScheduler].
 */
class AiringNotifWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
        val episode = inputData.getInt(KEY_EPISODE, -1)
        if (episode < 0) return@withContext Result.failure()
        val coverUrl = inputData.getString(KEY_COVER_URL)
        val notifId = inputData.getInt(KEY_NOTIF_ID, title.hashCode())

        AiringNotifier(context).showEpisodeAiredNotification(
            animeTitle = title,
            episodeNum = episode,
            coverUrl = coverUrl,
            notifId = notifId,
        )
        Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_EPISODE = "episode"
        const val KEY_COVER_URL = "coverUrl"
        const val KEY_NOTIF_ID = "notifId"

        /** Unique work-name prefix; appended with the anime title hash + episode number. */
        private const val WORK_NAME_PREFIX = "AiringNotif_"

        /**
         * Schedule a one-shot notification to fire when [airingAtEpochSeconds] is reached.
         * If a notification for this exact episode is already scheduled it is replaced.
         *
         * @param title              Anime display title.
         * @param episode            Episode number that will air.
         * @param coverUrl           Remote cover-art URL (may be null).
         * @param airingAtEpochSeconds  Unix timestamp (seconds) when the episode airs.
         */
        fun schedule(
            context: Context,
            title: String,
            episode: Int,
            coverUrl: String?,
            airingAtEpochSeconds: Long,
        ) {
            val delayMs = (airingAtEpochSeconds * 1_000L) - System.currentTimeMillis()
            // Don't schedule if the episode has already aired more than 1 hour ago.
            if (delayMs < -(60 * 60 * 1_000L)) return

            val effectiveDelay = delayMs.coerceAtLeast(0L)

            val data = Data.Builder()
                .putString(KEY_TITLE, title)
                .putInt(KEY_EPISODE, episode)
                .putString(KEY_COVER_URL, coverUrl)
                .putInt(KEY_NOTIF_ID, (title.hashCode() * 31) + episode)
                .build()

            val work = OneTimeWorkRequestBuilder<AiringNotifWorker>()
                .setInitialDelay(effectiveDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(title, episode),
                ExistingWorkPolicy.REPLACE,
                work,
            )
        }

        /**
         * Cancel a previously scheduled notification for this episode.
         */
        fun cancel(context: Context, title: String, episode: Int) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(title, episode))
        }

        /**
         * Cancel ALL pending airing notifications (e.g. when the user disables the feature).
         */
        fun cancelAll(context: Context) {
            // WorkManager does not support tag-based cancellation in all versions;
            // use per-work cancellation from the scheduler instead.
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }

        private fun workName(title: String, episode: Int) =
            "$WORK_NAME_PREFIX${title.hashCode()}_$episode"

        const val TAG = "AiringNotifWorker"
    }
}
