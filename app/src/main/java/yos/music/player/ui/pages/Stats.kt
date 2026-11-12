package yos.music.player.ui.pages

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.ListenStatsManager
import yos.music.player.code.MediaController
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.data.libraries.ListenStatsEvent
import yos.music.player.data.libraries.ListenStatsLibrary
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.SettingsLibrary
import yos.music.player.data.libraries.StatsAlbumEntry
import yos.music.player.data.libraries.StatsArtistEntry
import yos.music.player.data.libraries.StatsPeriod
import yos.music.player.data.libraries.StatsPeriodSnapshot
import yos.music.player.data.libraries.StatsTrackEntry
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.YosRoundedCornerShape
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ImageQuality
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.RollingNumberText
import yos.music.player.ui.widgets.basic.ShadowImageWithCache
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.YosWrapper
import java.util.Calendar

private const val STATS_ROW_MAX_ITEMS = 10
private const val STATS_LIVE_REFRESH_MS = 60_000L

fun selectedStatsPeriod(): StatsPeriod {
    val storedOrdinal = SettingsLibrary.StatsSelectedPeriod
    return StatsPeriod.entries.getOrElse(storedOrdinal) { StatsPeriod.Today }
}

@Composable
internal fun rememberStatsSnapshot(selectedPeriod: StatsPeriod = selectedStatsPeriod()): StatsPeriodSnapshot {
    val cacheVersion = ListenStatsManager.statsCacheVersion.intValue
    val liveRefreshVersion = remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        snapshotFlow { ListenStatsManager.liveSessionEvents.value.isNotEmpty() }
            .collectLatest { hasLiveEvents ->
                liveRefreshVersion.intValue++
                while (hasLiveEvents) {
                    delay(STATS_LIVE_REFRESH_MS)
                    liveRefreshVersion.intValue++
                }
            }
    }

    return produceState(
        ListenStatsLibrary.emptySnapshot,
        cacheVersion,
        selectedPeriod,
        liveRefreshVersion.intValue
    ) {
        val liveEvents = ListenStatsManager.liveSessionEvents.value
        value = withContext(Dispatchers.Default) {
            ListenStatsManager.snapshotForPeriod(selectedPeriod, liveEvents)
        }
    }.value
}

@Composable
fun Stats(navController: NavController) =
    Title(
        title = stringResource(id = R.string.page_stats_title),
        rightIconContent = {
            ProfileButton { navController.toUI(UI.Settings.Main) }
        },
        content = {
            item("StatsContent") {
                StatsContent(navController)
            }
        }
    )

@Composable
private fun StatsContent(navController: NavController) {
    YosWrapper {
        val selectedPeriod = selectedStatsPeriod()
        val statsSnapshot = rememberStatsSnapshot(selectedPeriod)

        val summary = statsSnapshot.summary
        val totalMinutes = summary.totalListenedMs / 60000L
        val artistEntries = statsSnapshot.artistEntries
        val albumEntries = statsSnapshot.albumEntries
        val trackEntries = statsSnapshot.trackEntries
        val artistRowState = rememberLazyListState()
        val albumRowState = rememberLazyListState()
        val trackRowState = rememberLazyListState()

        val allEvents = ListenStatsManager.statsEvents.value

        LaunchedEffect(selectedPeriod) {
            artistRowState.scrollToItem(0)
            albumRowState.scrollToItem(0)
            trackRowState.scrollToItem(0)
        }

        Column(Modifier.fillMaxWidth()) {
            // 1. Period Selector Pills
            StatsPeriodPills(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { SettingsLibrary.StatsSelectedPeriod = it.ordinal }
            )

            // 2. Main Stats Headline with Listening Streak
            StatsHeadlineWithStreak(
                totalMinutes = totalMinutes,
                playCount = summary.playCount,
                uniqueAlbumCount = summary.uniqueAlbumCount,
                selectedPeriod = selectedPeriod,
                allEvents = allEvents,
                hasData = statsSnapshot.hasEvents && totalMinutes > 0
            )

            // 3. ZERO STATE: When listening time is zero
            if (totalMinutes == 0L || !statsSnapshot.hasEvents) {
                QuietStageZeroState()
            } else {
                // 4. Weekly Activity Bar Chart
                WeeklyActivityChart(events = allEvents)

                // 5. Top Anthem Card (Apple Replay Hero Card)
                val topTrack = trackEntries.firstOrNull()
                if (topTrack != null) {
                    TopAnthemCard(
                        track = topTrack,
                        allTracks = trackEntries
                    )
                }

                // 6. Top Artists Section
                if (artistEntries.isNotEmpty()) {
                    StatsSectionHeader(title = stringResource(id = R.string.stats_artists_title)) {
                        navController.toUI(UI.StatsArtists)
                    }
                    LazyRow(
                        state = artistRowState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        items(
                            count = artistEntries.size.coerceAtMost(STATS_ROW_MAX_ITEMS),
                            key = { rowIndex -> artistEntries[rowIndex].artistName }
                        ) { rowIndex ->
                            val artistEntry = artistEntries[rowIndex]
                            StatsArtistCard(artistEntry = artistEntry) {
                                openStatsArtist(navController, artistEntry)
                            }
                        }
                    }
                }

                // 7. Top Albums Section
                if (albumEntries.isNotEmpty()) {
                    StatsSectionHeader(title = stringResource(id = R.string.stats_albums_title)) {
                        navController.toUI(UI.StatsAlbums)
                    }
                    LazyRow(
                        state = albumRowState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        items(
                            count = albumEntries.size.coerceAtMost(STATS_ROW_MAX_ITEMS),
                            key = { rowIndex -> albumEntries[rowIndex].albumName }
                        ) { rowIndex ->
                            val albumEntry = albumEntries[rowIndex]
                            StatsAlbumCard(albumEntry = albumEntry) {
                                openStatsAlbum(navController, albumEntry)
                            }
                        }
                    }
                }

                // 8. Top Tracks Section
                if (trackEntries.isNotEmpty()) {
                    StatsSectionHeader(title = stringResource(id = R.string.stats_tracks_title)) {
                        navController.toUI(UI.StatsTracks)
                    }
                    LazyRow(
                        state = trackRowState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        items(
                            count = trackEntries.size.coerceAtMost(STATS_ROW_MAX_ITEMS),
                            key = { rowIndex -> trackEntries[rowIndex].trackKey }
                        ) { rowIndex ->
                            val trackEntry = trackEntries[rowIndex]
                            StatsTrackCard(trackEntry = trackEntry, allTrackEntries = trackEntries)
                        }
                    }
                }

                // 9. Top Genres Distribution Chips
                TopGenresSection(trackEntries = trackEntries)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Zero State: Elegant Quiet Stage prompt with compact Shuffle Library action
 */
@Composable
private fun QuietStageZeroState() {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val containerBg = Color(0xFFF7F7FA) withNight Color(0xFF151724)
    val containerBorder = Color.Black.copy(alpha = 0.06f) withNight Color.White.copy(alpha = 0.10f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(containerBg)
            .border(0.6.dp, containerBorder, RoundedCornerShape(22.dp))
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(0.04f) withNight Color.White.copy(0.05f))
                .border(0.6.dp, Color.Black.copy(0.08f) withNight Color.White.copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.06f) withNight Color.White.copy(0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.button_icon_shuffle),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black withNight Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(id = R.string.stats_quiet_stage_title),
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = Color.Black withNight Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(id = R.string.stats_quiet_stage_subtitle),
            fontSize = 13.sp,
            color = (Color.Black withNight Color.White).copy(alpha = 0.5f),
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Luxury Crisp White Pill Button (Apple / Spotify Wrapped standard)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black withNight Color.White)
                .clickable {
                    if (songs.isNotEmpty()) {
                        Vibrator.click(context)
                        scope.launch(Dispatchers.IO) {
                            val shuffled = songs.shuffled()
                            MediaController.prepare(shuffled.first(), shuffled)
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.tip_no_song), Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.button_icon_shuffle),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = Color.White withNight Color(0xFF0C0D14)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.stats_shuffle_library),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White withNight Color(0xFF0C0D14)
            )
        }
    }
}

/**
 * Top Anthem Card (Apple Replay Hero Banner)
 */
@Composable
private fun TopAnthemCard(
    track: StatsTrackEntry,
    allTracks: List<StatsTrackEntry>
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cardBg = Color(0xFFF7F7FA) withNight Color(0xFF151724)
    val cardBorder = Color.Black.copy(alpha = 0.06f) withNight Color.White.copy(alpha = 0.10f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.stats_top_anthem),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(0.6.dp, cardBorder, RoundedCornerShape(18.dp))
                .clickable {
                    playStatsTrack(track, allTracks, scope, context)
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E202C))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.thumb)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_music_default_artwork)
                        .error(R.drawable.placeholder_music_default_artwork)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black withNight Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artistName} • ${track.albumName}",
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${track.listenedMs / 60000L} mins listened",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryDark
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black withNight Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nowplaying_mp_play),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White withNight Color(0xFF0C0D14)
                )
            }
        }
    }
}

/**
 * Weekly Activity Bar Chart
 */
@Composable
private fun WeeklyActivityChart(events: List<ListenStatsEvent>) {
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    val dailyMinutes = remember(events) {
        val calendar = Calendar.getInstance()
        val minutesPerDay = MutableList(7) { 0L }

        // Compute day offsets for the last 7 days
        val nowMs = System.currentTimeMillis()
        for (event in events) {
            val diffMs = nowMs - event.timestampMs
            val dayIndex = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
            if (dayIndex in 0..6) {
                val arrayIndex = 6 - dayIndex // 6 is today, 0 is 6 days ago
                minutesPerDay[arrayIndex] += (event.listenedMs / 60000L)
            }
        }
        minutesPerDay
    }

    val maxMinutes = remember(dailyMinutes) {
        dailyMinutes.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    }

    val chartBg = Color(0xFFF7F7F9) withNight Color(0xFF161619)
    val chartBorder = Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.07f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(chartBg)
            .border(0.6.dp, chartBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.stats_weekly_activity),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black withNight Color.White
            )
            Text(
                text = "${dailyMinutes.sum()} mins total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 7 Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyMinutes.forEachIndexed { index, minutes ->
                val ratio = (minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.06f, 1f)
                val isToday = index == 6

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height((60 * ratio).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isToday) {
                                    Brush.verticalGradient(listOf(Color(0xFFFF3358), Color(0xFFFF6B45)))
                                } else if (minutes > 0) {
                                    Brush.verticalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
                                } else {
                                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.08f)))
                                }
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dayNames[index],
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) (Color.Black withNight Color.White) else (Color.Black withNight Color.White).copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

/**
 * Top Genres Distribution Chips
 */
@Composable
private fun TopGenresSection(trackEntries: List<StatsTrackEntry>) {
    val topGenres = remember(trackEntries) {
        val genreCounts = mutableMapOf<String, Int>()
        for (track in trackEntries) {
            val genre = track.libraryItem?.genre?.takeIf { it.isNotBlank() } ?: continue
            genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
        }
        genreCounts.entries.sortedByDescending { it.value }.take(5).map { it.key }
    }

    if (topGenres.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.stats_top_genres),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topGenres) { genre ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF0F0F4) withNight Color(0xFF161824))
                        .border(0.6.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = genre,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black withNight Color.White
                    )
                }
            }
        }
    }
}

/**
 * Headline with Rolling Numbers and Streak Indicator
 */
@Composable
private fun StatsHeadlineWithStreak(
    totalMinutes: Long,
    playCount: Int,
    uniqueAlbumCount: Int,
    selectedPeriod: StatsPeriod,
    allEvents: List<ListenStatsEvent>,
    hasData: Boolean
) {
    val minutesLabel = stringResource(
        id = when (selectedPeriod) {
            StatsPeriod.Today -> R.string.stats_minutes_today
            StatsPeriod.ThisWeek -> R.string.stats_minutes_this_week
            StatsPeriod.ThisMonth -> R.string.stats_minutes_this_month
            StatsPeriod.ThisYear -> R.string.stats_minutes_this_year
            StatsPeriod.AllTime -> R.string.stats_minutes_all_time
        }
    )

    // Compute consecutive day streak
    val streakDays = remember(allEvents) {
        if (allEvents.isEmpty()) 0
        else {
            val dayTimestamps = allEvents.map { it.dayStartMs }.distinct().sortedDescending()
            var streak = 0
            val oneDayMs = 24 * 60 * 60 * 1000L
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            var expectedDay = if (dayTimestamps.contains(todayStart)) todayStart else todayStart - oneDayMs
            for (day in dayTimestamps) {
                if (day == expectedDay) {
                    streak++
                    expectedDay -= oneDayMs
                } else if (day < expectedDay) {
                    break
                }
            }
            streak
        }
    }

    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (streakDays > 1) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x22FFA502))
                    .border(0.6.dp, Color(0x55FFA502), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 " + stringResource(id = R.string.stats_streak_days, streakDays),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9F1A)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumberText(
                targetValue = totalMinutes,
                style = TextStyle(fontSize = 58.sp, fontWeight = FontWeight.Bold, lineHeight = 62.sp)
            )
            Text(
                text = minutesLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 10.dp, bottom = 9.dp)
                    .alpha(0.8f)
            )
        }

        if (hasData) {
            Text(
                text = stringResource(
                    id = R.string.stats_across_summary,
                    String.format("%,d", playCount),
                    String.format("%,d", uniqueAlbumCount)
                ),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(0.6f)
            )
        }
    }
}

internal fun openStatsArtist(navController: NavController, artistEntry: StatsArtistEntry) {
    if (!artistEntry.inLibrary) return
    LibraryObject.setTargetArtistName(artistEntry.artistName)
    LibraryObject.setArtistSongsSearchOnOpen(false)
    navController.toUI(UI.ArtistInfo)
}

internal fun openStatsAlbum(navController: NavController, albumEntry: StatsAlbumEntry) {
    if (!albumEntry.inLibrary) return
    LibraryObject.setTargetAlbumName(albumEntry.albumName)
    navController.toUI(UI.AlbumInfo)
}

@Composable
private fun StatsPeriodPills(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    val pillLabels = listOf(
        StatsPeriod.Today to stringResource(id = R.string.stats_period_today),
        StatsPeriod.ThisWeek to stringResource(id = R.string.stats_period_this_week),
        StatsPeriod.ThisMonth to stringResource(id = R.string.stats_period_this_month),
        StatsPeriod.ThisYear to stringResource(id = R.string.stats_period_this_year),
        StatsPeriod.AllTime to stringResource(id = R.string.stats_period_all_time)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pillLabels.forEach { (period, pillLabel) ->
            val isSelected = period == selectedPeriod
            val pillShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .background(
                        if (isSelected) {
                            Color.Black withNight Color.White
                        } else {
                            Color(0xFFF0F0F4) withNight Color(0xFF161824)
                        }
                    )
                    .border(
                        0.6.dp,
                        if (isSelected) Color.Transparent else (Color.Black.copy(0.06f) withNight Color.White.copy(0.10f)),
                        pillShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPeriodSelected(period) }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = pillLabel,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        Color.White withNight Color(0xFF0C0D14)
                    } else {
                        Color.Black.copy(0.7f) withNight Color(0xFFD6D6DE)
                    }
                )
            }
        }
    }
}

@Composable
internal fun StatsSectionHeader(title: String, onMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMore
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_next),
                contentDescription = title,
                modifier = Modifier
                    .size(16.dp)
                    .alpha(0.55f),
                tint = Color.Black withNight Color.White
            )
        }
    }
}

@Composable
internal fun StatsMinutesText(listenedMs: Long, fontSize: Int = 12) {
    Text(
        text = stringResource(id = R.string.stats_minutes_label, String.format("%,d", listenedMs / 60000L)),
        fontSize = fontSize.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
    )
}

@Composable
internal fun StatsArtistCard(artistEntry: StatsArtistEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
    ) {
        ShadowImageWithCache(
            dataLambda = { artistEntry.thumb },
            contentDescription = artistEntry.artistName,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (artistEntry.inLibrary) 1f else 0.4f),
            cornerRadius = 12.dp,
            shadowAlpha = 0f,
            imageQuality = ImageQuality.HIGH
        )
        Text(
            text = artistEntry.artistName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )
        Box(Modifier.padding(top = 2.dp)) {
            StatsMinutesText(listenedMs = artistEntry.listenedMs)
        }
    }
}

@Composable
internal fun StatsAlbumCard(albumEntry: StatsAlbumEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
    ) {
        ShadowImageWithCache(
            dataLambda = { albumEntry.thumb },
            contentDescription = albumEntry.albumName,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (albumEntry.inLibrary) 1f else 0.4f),
            cornerRadius = 12.dp,
            shadowAlpha = 0f,
            imageQuality = ImageQuality.HIGH
        )
        Text(
            text = albumEntry.albumName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = albumEntry.artistName,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = (Color.Black withNight Color.White).copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
        Box(Modifier.padding(top = 2.dp)) {
            StatsMinutesText(listenedMs = albumEntry.listenedMs)
        }
    }
}

@Composable
internal fun StatsTrackCard(trackEntry: StatsTrackEntry, allTrackEntries: List<StatsTrackEntry>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable {
                playStatsTrack(trackEntry, allTrackEntries, scope, context)
            }
    ) {
        ShadowImageWithCache(
            dataLambda = { trackEntry.thumb },
            contentDescription = trackEntry.title,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (trackEntry.libraryItem != null) 1f else 0.4f),
            cornerRadius = 12.dp,
            shadowAlpha = 0f,
            imageQuality = ImageQuality.HIGH
        )
        Text(
            text = trackEntry.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = trackEntry.artistName,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = (Color.Black withNight Color.White).copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
        Box(Modifier.padding(top = 2.dp)) {
            StatsMinutesText(listenedMs = trackEntry.listenedMs)
        }
    }
}

internal fun playStatsTrack(
    trackEntry: StatsTrackEntry,
    allTrackEntries: List<StatsTrackEntry>,
    scope: CoroutineScope,
    context: android.content.Context
) {
    val targetItem = trackEntry.libraryItem
    if (targetItem == null) {
        Toast.makeText(context, context.getString(R.string.stats_track_missing_toast), Toast.LENGTH_SHORT).show()
        return
    }

    val playableList = allTrackEntries.mapNotNull { it.libraryItem }
    scope.launch(Dispatchers.IO) {
        MediaController.prepare(targetItem, playableList)
    }
}
