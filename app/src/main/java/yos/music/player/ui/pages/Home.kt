package yos.music.player.ui.pages

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.ListenHistoryManager
import yos.music.player.code.MediaController
import yos.music.player.code.utils.others.BitmapResolver
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.code.utils.player.FadeExo.fadePause
import yos.music.player.code.utils.player.FadeExo.fadePlay
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.SettingsLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultAlbum
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle
import yos.music.player.data.models.ImageViewModel
import yos.music.player.data.objects.LibraryObject
import yos.music.player.data.objects.MediaViewModelObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.YosRoundedCornerShape
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.YosWrapper
import yos.music.player.ui.widgets.effects.imageResolve
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun Home(
    navController: NavController,
    imageViewModel: ImageViewModel
) {
    val musicList = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())

    Title(
        title = stringResource(id = R.string.page_home_title),
        rightIconContent = {
            ProfileButton { navController.toUI(UI.Settings.Main) }
        },
        content = {
            // 1. Dynamic Time-Aware Greeting Header
            item("DynamicHeader") {
                DynamicGreetingHeader(songCount = musicList.size)
            }

            // 2. Jump Back In (Resume Session Card)
            item("JumpBackIn") {
                JumpBackInCard()
            }

            // 3. Try These (Hero Recommendation Pager)
            item("RecommendCard") {
                RecommendCard(imageViewModel)
            }

            // 4. Quick Picks (3-Row Vertical Track Layout)
            item("QuickPicks") {
                QuickPicksSection()
            }

            // 5. Heavy Rotation (2-Row Horizontal Album Grid)
            item("HeavyRotation") {
                HeavyRotationSection(navController)
            }

            // 6. Recently Added (Horizontal Album Shelf)
            item("RecentlyAdded") {
                RecentlyAddedShelf(navController)
            }

            // 7. Listening History (if available)
            item("RecentlyPlayedCard") {
                RecentlyPlayedCard()
            }
        }
    )
}

/**
 * 1. Time-Aware Dynamic Header with refined typography and subtle contextual info.
 */
@Composable
private fun DynamicGreetingHeader(songCount: Int) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }

    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            text = formattedDate.uppercase(Locale.getDefault()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = greeting),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black withNight Color.White
            )
            if (songCount > 0) {
                Text(
                    text = "$songCount tracks",
                    fontSize = 12.sp,
                    color = (Color.DarkGray withNight Color.LightGray).copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 2. Jump Back In: Resume the last active session with album art and quick-play button.
 */
@Composable
private fun JumpBackInCard() {
    val currentPlaying = MediaController.musicPlaying.value
    val recentSongs = ListenHistoryManager.recentlyPlayedSongs.value
    val targetSong = currentPlaying ?: recentSongs.firstOrNull() ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPlaying = MediaViewModelObject.isPlaying.value && currentPlaying != null

    val cardBg = Color(0xFFF6F6F8) withNight Color(0xFF161619)
    val cardBorder = Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_jump_back_in),
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(0.6.dp, cardBorder, RoundedCornerShape(16.dp))
                .clickable {
                    scope.launch(Dispatchers.IO) {
                        if (currentPlaying?.uri == targetSong.uri) {
                            if (isPlaying) {
                                MediaController.mediaControl?.fadePause()
                            } else {
                                MediaController.mediaControl?.fadePlay()
                            }
                        } else {
                            val list = if (recentSongs.isNotEmpty()) recentSongs else listOf(targetSong)
                            MediaController.prepare(targetSong, list)
                        }
                    }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with smooth rounded corner
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(targetSong.thumb)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_music_default_artwork)
                    .error(R.drawable.placeholder_music_default_artwork)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = targetSong.title ?: defaultTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black withNight Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${targetSong.artistsName ?: defaultArtistsName} • ${targetSong.album ?: defaultAlbum}",
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.55f)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Play / Pause Circle Action Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                if (currentPlaying?.uri == targetSong.uri) {
                                    if (isPlaying) {
                                        MediaController.mediaControl?.fadePause()
                                    } else {
                                        MediaController.mediaControl?.fadePlay()
                                    }
                                } else {
                                    val list = if (recentSongs.isNotEmpty()) recentSongs else listOf(targetSong)
                                    MediaController.prepare(targetSong, list)
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying && currentPlaying?.uri == targetSong.uri) {
                            R.drawable.ic_nowplaying_mp_pause
                        } else {
                            R.drawable.ic_nowplaying_mp_play
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 3. Quick Picks: 3-Row vertical song stacks inside a horizontal pager.
 */
@Composable
private fun QuickPicksSection() {
    val songs = remember {
        val all = MusicLibrary.songs
        if (all.size <= 9) all else all.shuffled().take(9)
    }

    if (songs.size < 3) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = remember(songs) { songs.chunked(3) }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.home_quick_picks),
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.Black withNight Color.White
            )
            Text(
                text = "START A STATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(320.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val pageSongs = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pageSongs.forEach { song ->
                    QuickPickItem(
                        song = song,
                        onClick = {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(song, songs)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPickItem(
    song: YosMediaItem,
    onClick: () -> Unit
) {
    val rowBg = Color(0xFFF7F7F9) withNight Color(0xFF141416)
    val borderCol = Color.Black.copy(alpha = 0.04f) withNight Color.White.copy(alpha = 0.06f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .border(0.5.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.thumb)
                .crossfade(true)
                .placeholder(R.drawable.placeholder_music_default_artwork)
                .error(R.drawable.placeholder_music_default_artwork)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title ?: defaultTitle,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black withNight Color.White
            )
            Text(
                text = song.artistsName ?: defaultArtistsName,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_nowplaying_mp_play),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .alpha(0.6f),
            tint = Color.Black withNight Color.White
        )
    }
}

/**
 * 4. Heavy Rotation: 2-Row Grid of most played or prominent albums.
 */
@Composable
private fun HeavyRotationSection(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    if (songs.isEmpty()) return

    val heavyRotationAlbums = remember(songs) {
        // Group songs by album, sort by track count descending
        songs.groupBy { it.album ?: defaultAlbum }
            .entries
            .sortedByDescending { it.value.size }
            .take(6)
            .map { entry ->
                entry.key to entry.value
            }
    }

    if (heavyRotationAlbums.isEmpty()) return
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_heavy_rotation),
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(heavyRotationAlbums) { (albumName, albumSongs) ->
                val firstSong = albumSongs.firstOrNull()
                Column(
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            Vibrator.click(context)
                            LibraryObject.setTargetAlbumName(albumName)
                            navController.toUI(UI.AlbumInfo)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202024))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(firstSong?.thumb)
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_music_default_artwork)
                                .error(R.drawable.placeholder_music_default_artwork)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = albumName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black withNight Color.White
                    )
                    Text(
                        text = firstSong?.artistsName ?: defaultArtistsName,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 5. Recently Added: Horizontal album shelf sorted by added timestamp.
 */
@Composable
private fun RecentlyAddedShelf(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    if (songs.isEmpty()) return

    val recentAlbums = remember(songs) {
        songs.sortedByDescending { it.addDate ?: 0L }
            .distinctBy { it.album ?: defaultAlbum }
            .take(8)
    }

    if (recentAlbums.isEmpty()) return
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.home_recently_added),
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.Black withNight Color.White
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(recentAlbums) { song ->
                Column(
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            Vibrator.click(context)
                            LibraryObject.setTargetAlbumName(song.album ?: defaultAlbum)
                            navController.toUI(UI.AlbumInfo)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202024))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.thumb)
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_music_default_artwork)
                                .error(R.drawable.placeholder_music_default_artwork)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = song.album ?: defaultAlbum,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black withNight Color.White
                    )
                    Text(
                        text = song.artistsName ?: defaultArtistsName,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Hero Recommendations Carousel ("Try These").
 */
@Composable
fun RecommendCard(imageViewModel: ImageViewModel) {
    val musicList = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())

    val showRecommend = remember(musicList) {
        derivedStateOf { musicList.isNotEmpty() }
    }

    if (showRecommend.value) {
        val randomMusicList = remember("RecommendCard_randomMusicList") {
            imageViewModel.recommendMusicList
        }

        YosWrapper {
            LaunchedEffect(showRecommend.value) {
                if (randomMusicList.value.isNotEmpty()) return@LaunchedEffect
                randomMusicList.value = musicList.pickRandomSongs(6)
            }
        }

        val pagerState = rememberPagerState(pageCount = { randomMusicList.value.size })

        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_recommend_title),
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                lineHeight = 19.sp,
                color = Color.Black withNight Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            val scope = rememberCoroutineScope()

            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(280.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 60.dp),
                key = { pageIndex -> "$pageIndex:${randomMusicList.value.getOrNull(pageIndex)?.uri}" },
                beyondViewportPageCount = 1
            ) { page ->
                val music = randomMusicList.value.getOrNull(page) ?: return@HorizontalPager
                RecommendCardItem(
                    music = music,
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            MediaController.prepare(music, randomMusicList.value)
                        }
                    }
                )
            }
        }
    }
}

private fun List<YosMediaItem>.pickRandomSongs(count: Int): List<YosMediaItem> {
    if (size <= count) return shuffled()
    val selectedIndexes = LinkedHashSet<Int>()
    while (selectedIndexes.size < count) {
        selectedIndexes.add(indices.random())
    }
    return selectedIndexes.map { this[it] }
}

sealed class RecentlyPlayedDisplayItem {
    data class Song(val song: YosMediaItem) : RecentlyPlayedDisplayItem()
    data class AlbumGroup(
        val albumName: String,
        val artistName: String,
        val artworkUri: android.net.Uri?,
        val songs: List<YosMediaItem>
    ) : RecentlyPlayedDisplayItem()
}

fun buildRecentlyPlayedDisplayItems(
    rawHistory: List<YosMediaItem>,
    maximumDisplayCount: Int = 10
): List<RecentlyPlayedDisplayItem> {
    if (rawHistory.isEmpty()) return emptyList()

    val groupedItems = mutableListOf<RecentlyPlayedDisplayItem>()
    var currentIndex = 0
    while (currentIndex < rawHistory.size) {
        val currentSong = rawHistory[currentIndex]
        val currentAlbum = currentSong.album ?: defaultAlbum
        var consecutiveCount = 1
        while (currentIndex + consecutiveCount < rawHistory.size &&
            (rawHistory[currentIndex + consecutiveCount].album ?: defaultAlbum) == currentAlbum
        ) {
            consecutiveCount++
        }

        if (consecutiveCount >= 4) {
            val albumSongs = rawHistory.subList(currentIndex, currentIndex + consecutiveCount)
            groupedItems.add(
                RecentlyPlayedDisplayItem.AlbumGroup(
                    albumName = currentAlbum,
                    artistName = currentSong.albumArtists ?: currentSong.artistsName ?: defaultArtistsName,
                    artworkUri = currentSong.thumb,
                    songs = albumSongs
                )
            )
        } else {
            for (songOffset in 0 until consecutiveCount) {
                groupedItems.add(RecentlyPlayedDisplayItem.Song(rawHistory[currentIndex + songOffset]))
            }
        }
        currentIndex += consecutiveCount
        if (groupedItems.size >= maximumDisplayCount) break
    }

    return groupedItems.take(maximumDisplayCount)
}

@Composable
fun RecentlyPlayedCard() {
    val recentlyPlayedHistory = ListenHistoryManager.recentlyPlayedSongs.value
    val listenHistoryEnabled = SettingsLibrary.ListenHistory

    if (!listenHistoryEnabled || recentlyPlayedHistory.isEmpty()) return

    val displayItems = remember(recentlyPlayedHistory) {
        buildRecentlyPlayedDisplayItems(recentlyPlayedHistory)
    }

    if (displayItems.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { displayItems.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_recently_played_title),
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 19.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(280.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 60.dp),
            key = { pageIndex ->
                when (val displayItem = displayItems[pageIndex]) {
                    is RecentlyPlayedDisplayItem.Song -> "$pageIndex:song:${displayItem.song.uri}"
                    is RecentlyPlayedDisplayItem.AlbumGroup -> "$pageIndex:album:${displayItem.albumName}"
                }
            },
            beyondViewportPageCount = 1
        ) { page ->
            when (val displayItem = displayItems[page]) {
                is RecentlyPlayedDisplayItem.Song -> {
                    val songsOnlyFromDisplayItems = displayItems.filterIsInstance<RecentlyPlayedDisplayItem.Song>().map { it.song }
                    RecommendCardItem(
                        music = displayItem.song,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(
                                    displayItem.song,
                                    songsOnlyFromDisplayItems
                                )
                            }
                        }
                    )
                }
                is RecentlyPlayedDisplayItem.AlbumGroup -> {
                    RecentlyPlayedAlbumCardItem(
                        albumGroup = displayItem,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val fullAlbumSongs = runCatching { MusicLibrary.Album[displayItem.albumName] }.getOrDefault(emptyList())
                                    .sortedWith(compareBy({ it.discNumber ?: 0 }, { it.trackNumber ?: 0 }))
                                if (fullAlbumSongs.isNotEmpty()) {
                                    MediaController.prepare(
                                        fullAlbumSongs.first(),
                                        fullAlbumSongs
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentlyPlayedAlbumCardItem(
    albumGroup: RecentlyPlayedDisplayItem.AlbumGroup,
    onClick: () -> Unit
) = Column(
    Modifier
        .width(268.dp)
        .padding(end = 12.dp)
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(shape)
            .background(Color(0xFF1B1B1E))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(albumGroup.artworkUri)
                .crossfade(true)
                .error(R.drawable.placeholder_music_default_artwork)
                .placeholder(R.drawable.placeholder_music_default_artwork)
                .fallback(R.drawable.placeholder_music_default_artwork)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = albumGroup.albumName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = albumGroup.artistName,
                fontSize = 12.5.sp,
                lineHeight = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecommendCardItem(music: YosMediaItem, onClick: () -> Unit) =
    Column(
        Modifier
            .width(268.dp)
            .padding(end = 12.dp)
    ) {
        val shape = RoundedCornerShape(16.dp)

        Column(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(shape)
                .background(Color(0xFF1B1B1E))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(music.thumb)
                    .crossfade(true)
                    .error(R.drawable.placeholder_music_default_artwork)
                    .placeholder(R.drawable.placeholder_music_default_artwork)
                    .fallback(R.drawable.placeholder_music_default_artwork)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = music.title ?: defaultTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${music.artistsName ?: defaultArtistsName} • ${music.album ?: defaultAlbum}",
                    fontSize = 12.5.sp,
                    lineHeight = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
