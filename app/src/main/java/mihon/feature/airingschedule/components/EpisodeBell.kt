package mihon.feature.airingschedule.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How the user currently wants to be alerted about a show's episodes. */
enum class BellNotifyState { NONE, ONCE, SERIES }

private const val LONG_PRESS_DURATION_MS = 1_000L

/**
 * Per-anime notification bell shown on the Schedule tab.
 *
 * - A quick tap toggles a one-off alert for the next upcoming episode ([BellNotifyState.ONCE]).
 * - Pressing and holding for 1 second toggles recurring alerts for every future episode of the
 *   series ([BellNotifyState.SERIES]).
 *
 * Tint: NONE = muted, ONCE = primary color, SERIES = tertiary color.
 * A circular progress arc grows during a long-press to give visual feedback.
 */
@Composable
fun EpisodeBell(
    state: BellNotifyState,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pressProgressState = remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = pressProgressState.floatValue,
        label = "bellPressProgress",
    )

    val neutralTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val onceTint = MaterialTheme.colorScheme.primary
    val seriesTint = MaterialTheme.colorScheme.tertiary

    val tint = when (state) {
        BellNotifyState.NONE -> neutralTint
        BellNotifyState.ONCE -> onceTint
        BellNotifyState.SERIES -> seriesTint
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .pointerInput(onTap, onLongPress) {
                coroutineScope {
                    while (true) {
                        awaitPointerEventScope {
                            awaitFirstDown(pass = PointerEventPass.Main)
                        }
                        var longPressFired = false
                        val progressJob = launch {
                            val steps = 40
                            val stepDelay = LONG_PRESS_DURATION_MS / steps
                            for (i in 1..steps) {
                                delay(stepDelay)
                                pressProgressState.floatValue = i / steps.toFloat()
                            }
                            longPressFired = true
                            onLongPress()
                        }
                        awaitPointerEventScope {
                            waitForUpOrCancellation()
                        }
                        progressJob.cancel()
                        pressProgressState.floatValue = 0f
                        if (!longPressFired) {
                            onTap()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (state == BellNotifyState.NONE) {
                Icons.Outlined.NotificationsNone
            } else {
                Icons.Filled.Notifications
            },
            contentDescription = when (state) {
                BellNotifyState.NONE -> "Notify me about this episode"
                BellNotifyState.ONCE -> "Notifying for next episode — hold 1s for every episode"
                BellNotifyState.SERIES -> "Notifying every episode until series finishes"
            },
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        if (animatedProgress > 0f) {
            Canvas(modifier = Modifier.size(30.dp)) {
                drawArc(
                    color = seriesTint,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}
