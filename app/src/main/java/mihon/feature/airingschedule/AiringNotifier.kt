package mihon.feature.airingschedule

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify

/**
 * Builds and posts a modern "Episode just aired" heads-up notification for the
 * Airing Schedule feature.
 *
 * Visual design:
 *  - Collapsed  : title + "Episode N just aired" + high-res thumbnail on the right.
 *  - Expanded   : full-width BigPicture of the anime cover (512 × 768 px, rounded corners).
 *  - Accent bar : app primary green tint on the small icon.
 *  - Priority   : HIGH so Android shows it as a heads-up popup.
 */
class AiringNotifier(private val context: Context) {

    /**
     * Post the notification immediately.
     *
     * @param animeTitle  Display title of the anime.
     * @param episodeNum  Episode number that just aired.
     * @param coverUrl    Remote URL for the anime cover art (can be null).
     * @param notifId     Unique notification ID (use anime id hash to group per-anime).
     */
    suspend fun showEpisodeAiredNotification(
        animeTitle: String,
        episodeNum: Int,
        coverUrl: String?,
        notifId: Int,
    ) {
        val coverBitmap: Bitmap? = coverUrl?.let { loadHighResCover(it) }

        context.notify(notifId, Notifications.CHANNEL_AIRING_EPISODES) {
            setSmallIcon(R.mipmap.ic_launcher)
            setColor(context.getColor(R.color.accent_blue))
            setContentTitle(animeTitle)
            setContentText("Episode $episodeNum just aired")
            priority = NotificationCompat.PRIORITY_HIGH

            setAutoCancel(true)
            setOnlyAlertOnce(false)

            if (coverBitmap != null) {
                // Collapsed view: show the cover as a large icon thumbnail on the right.
                setLargeIcon(coverBitmap)

                // Expanded view: show the full-width cover banner.
                setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(coverBitmap)
                        // Hide duplicate large icon when expanded so the banner fills the space.
                        .bigLargeIcon(null as Bitmap?)
                        .setBigContentTitle(animeTitle)
                        .setSummaryText("Episode $episodeNum just aired"),
                )
            } else {
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Episode $episodeNum just aired")
                        .setBigContentTitle(animeTitle),
                )
            }

            // Tap → open the app on the Updates tab.
            setContentIntent(buildOpenAppIntent(notifId))
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    /**
     * Downloads the cover image at a resolution large enough to fill the
     * BigPicture expanded area without looking pixelated.
     */
    private suspend fun loadHighResCover(url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(COVER_WIDTH_PX, COVER_HEIGHT_PX)
            .transformations(RoundedCornersTransformation(COVER_CORNER_RADIUS_PX))
            .build()
        val drawable = context.imageLoader.execute(request).image
            ?.asDrawable(context.resources)
        return drawable?.getBitmapOrNull()
    }

    private fun buildOpenAppIntent(notifId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Deep-link to the Updates tab so the user can find the new episode.
            action = "eu.kanade.tachiyomi.SHORTCUT_UPDATES"
        }
        return PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Width of the cover image fetched for both large-icon and BigPicture. */
        private const val COVER_WIDTH_PX = 512

        /** Height kept at 3:2 portrait ratio — looks good in the expanded banner. */
        private const val COVER_HEIGHT_PX = 768

        /** Corner radius applied to the cover bitmap (in pixels). */
        private const val COVER_CORNER_RADIUS_PX = 16f
    }
}
