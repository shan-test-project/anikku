package mihon.feature.airingschedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mihon.feature.airingschedule.AiringScheduleEntry
import mihon.feature.airingschedule.SchedulePreferences
import mihon.feature.airingschedule.UploadDelayTracker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ScheduleAnimeCard(
    entry: AiringScheduleEntry,
    titleLanguage: SchedulePreferences.TitleLanguage,
    sourceDelays: Map<String, Long>,
    favoriteSourceIds: Set<String>,
    onSearchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val displayTitle = entry.displayTitle(titleLanguage)
    val officialAirTime = Instant.ofEpochSecond(entry.airingAt).atZone(zone).format(timeFormatter)
    val hasAired = entry.hasAired()

    val avgDelayMinutes: Long? = if (sourceDelays.isNotEmpty() && favoriteSourceIds.isNotEmpty()) {
        val relevant = sourceDelays.entries.filter { it.key in favoriteSourceIds }.map { it.value }
        if (relevant.isEmpty()) null else relevant.average().toLong()
    } else {
        null
    }

    val expectedUploadTime: String? = avgDelayMinutes?.let {
        Instant.ofEpochSecond(UploadDelayTracker.adjustedAirTime(entry.airingAt, it))
            .atZone(zone)
            .format(timeFormatter)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .clickable { onSearchClick(displayTitle) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimeCardCoverImage(entry = entry, displayTitle = displayTitle)
            AnimeCardInfo(
                displayTitle = displayTitle,
                officialAirTime = officialAirTime,
                hasAired = hasAired,
                expectedUploadTime = expectedUploadTime,
                entry = entry,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(2.dp))
            AnimeCardActions(
                hasAired = hasAired,
                displayTitle = displayTitle,
                onSearchClick = onSearchClick,
            )
        }
    }
}

@Composable
private fun AnimeCardCoverImage(entry: AiringScheduleEntry, displayTitle: String) {
    Box {
        AsyncImage(
            model = entry.coverImageUrl,
            contentDescription = displayTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        if (entry.isAdult) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 6.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "18+",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AnimeCardInfo(
    displayTitle: String,
    officialAirTime: String,
    hasAired: Boolean,
    expectedUploadTime: String?,
    entry: AiringScheduleEntry,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnimeCardTimingRow(
            officialAirTime = officialAirTime,
            hasAired = hasAired,
            expectedUploadTime = expectedUploadTime,
            episode = entry.episode,
            totalEpisodes = entry.totalEpisodes,
        )
        Spacer(modifier = Modifier.height(2.dp))
        AnimeCardMetaRow(format = entry.format, averageScore = entry.averageScore)
    }
}

@Composable
private fun AnimeCardTimingRow(
    officialAirTime: String,
    hasAired: Boolean,
    expectedUploadTime: String?,
    episode: Int,
    totalEpisodes: Int?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (hasAired) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = if (hasAired) "Aired $officialAirTime" else officialAirTime,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (hasAired) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (expectedUploadTime != null && expectedUploadTime != officialAirTime) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "Src ~$expectedUploadTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        val episodeText = if (totalEpisodes != null) "Ep $episode / $totalEpisodes" else "Ep $episode"
        Text(
            text = episodeText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnimeCardMetaRow(format: String?, averageScore: Int?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        format?.let { fmt ->
            Text(
                text = fmt.replace("_", " "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        averageScore?.let { score ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "${score / 10.0}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun AnimeCardActions(
    hasAired: Boolean,
    displayTitle: String,
    onSearchClick: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(
            onClick = { onSearchClick(displayTitle) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = if (hasAired) Icons.Outlined.PlayCircle else Icons.Outlined.Search,
                contentDescription = if (hasAired) "Watch / Find episode" else "Find in sources",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            onClick = { onSearchClick(displayTitle) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = "Add to library",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
