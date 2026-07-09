package mihon.feature.airingschedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mihon.feature.airingschedule.SchedulePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Fires the actual system notification once an alarm scheduled by [ScheduleNotifications]
 * reaches its trigger time. Styled to match the app's own notification look (accent color,
 * app icon, cover art as the large icon) rather than a bare system default notification.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mediaId = intent.getIntExtra(EXTRA_MEDIA_ID, -1)
        val episode = intent.getIntExtra(EXTRA_EPISODE, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val coverUrl = intent.getStringExtra(EXTRA_COVER_URL)
        if (mediaId == -1 || episode == -1) return

        val schedulePreferences: SchedulePreferences = Injekt.get()
        val key = ScheduleNotifications.alarmKey(mediaId, episode)
        schedulePreferences.scheduledAlarmKeys().set(schedulePreferences.scheduledAlarmKeys().get() - key)

        // Cover-art loading is async (network/disk); use goAsync() so the receiver's process
        // isn't killed before the notification is actually posted.
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val coverBitmap = coverUrl?.let { loadCoverBitmap(appContext, it) }
            postNotification(appContext, mediaId, episode, title, coverBitmap)
            pendingResult.finish()
        }
    }

    private suspend fun loadCoverBitmap(context: Context, url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context).data(url).build()
        context.imageLoader.execute(request).image?.asDrawable(context.resources)?.toBitmap()
    }.getOrNull()

    private fun postNotification(
        context: Context,
        mediaId: Int,
        episode: Int,
        title: String,
        coverBitmap: Bitmap?,
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.INTENT_SEARCH_QUERY, title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = android.app.PendingIntent.getActivity(
            context,
            ScheduleNotifications.requestCode(mediaId, episode),
            contentIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = context.getString(R.string.notification_episode_aired, episode)

        // Use bitmasking instead of kotlin.math.abs to safely strip the sign bit.
        // abs(Int.MIN_VALUE) overflows back to Int.MIN_VALUE, so bitmask is the safe approach.
        val notificationId = Notifications.ID_AIRING_SCHEDULE_BASE -
            ((ScheduleNotifications.requestCode(mediaId, episode) and Int.MAX_VALUE) % 10000)

        runCatching {
            context.notify(notificationId, Notifications.CHANNEL_AIRING_SCHEDULE) {
                setSmallIcon(R.drawable.ic_komikku)
                setContentTitle(title)
                setContentText(contentText)
                setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                coverBitmap?.let { setLargeIcon(it) }
                setPriority(NotificationCompat.PRIORITY_HIGH)
                setCategory(NotificationCompat.CATEGORY_REMINDER)
                setAutoCancel(true)
                setContentIntent(pendingContentIntent)
            }
        }
    }

    companion object {
        const val EXTRA_MEDIA_ID = "media_id"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_TITLE = "title"
        const val EXTRA_COVER_URL = "cover_url"
    }
}
