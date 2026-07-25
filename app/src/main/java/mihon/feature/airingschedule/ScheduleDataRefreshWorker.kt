package mihon.feature.airingschedule

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ScheduleDataRefreshWorker {
    private const val WORK_NAME = "ScheduleDataRefreshWorker"

    fun schedule(context: Context, frequency: SchedulePreferences.AutoRefreshFrequency) {
        val days = when (frequency) {
            SchedulePreferences.AutoRefreshFrequency.EVERY_1_DAY -> 1L
            SchedulePreferences.AutoRefreshFrequency.EVERY_2_DAYS -> 2L
            SchedulePreferences.AutoRefreshFrequency.EVERY_3_DAYS -> 3L
            SchedulePreferences.AutoRefreshFrequency.EVERY_4_DAYS -> 4L
            SchedulePreferences.AutoRefreshFrequency.EVERY_5_DAYS -> 5L
            SchedulePreferences.AutoRefreshFrequency.EVERY_6_DAYS -> 6L
            SchedulePreferences.AutoRefreshFrequency.EVERY_7_DAYS -> 7L
        }
        val request = PeriodicWorkRequestBuilder<ScheduleRefreshWorker>(days, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
