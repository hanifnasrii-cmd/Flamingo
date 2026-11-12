package yos.music.player.ui.pages

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yos.music.player.R
import yos.music.player.code.ListenHistoryManager
import yos.music.player.code.MediaController
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.code.utils.player.FadeExo.fadePause
import yos.music.player.code.utils.player.FadeExo.fadePlay
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultAlbum
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle
import yos.music.player.data.models.ImageViewModel
import yos.music.player.data.objects.LibraryObject
import yos.music.player.data.objects.MediaViewModelObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.primaryDark
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

            // 4. Quick Picks (Unified 3-Track Glass Container)
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

            item("BottomSpacer") {
                Spacer(modifier = Modifier.height(28.dp))
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
        val infoText = if (songCount > 0) {
            "${formattedDate.uppercase(Locale.getDefault())} • $songCount TRACKS"
        } else {
            formattedDate.uppercase(Locale.getDefault())
        }

        Text(
            text = infoText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = (Color.Black withNight Color.White).copy(alpha = 0.5f),
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = stringResource(id = greeting),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black withNight Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Jump back into your favorites or discover fresh sound.",
            fontSize = 12.5.sp,
            color = (Color.Black withNight Color.White).copy(alpha = 0.45f),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 2. Jump Back In: Resume the last active session with album art and crisp white play button.
 */
@Composable
private fun JumpBackInCard() {
    val currentPlaying = MediaController.musicPlaying.value
    val recentSongs = ListenHistoryManager.recentlyPlayedSongs.value
    val targetSong = currentPlaying ?: recentSongs.firstOrNull() ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPlaying = MediaViewModelObject.isPlaying.value && currentPlaying != null

    val cardBg = Color(0xFFF7F7FA) withNight Color(0xFF151724)
    val cardBorder = Color.Black.copy(alpha = 0.06f) withNight Color.White.copy(alpha = 0.09f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
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
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(0.6.dp, cardBorder, RoundedCornerShape(18.dp))
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with smooth rounded corners
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E202C))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(targetSong.thumb)
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

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "RESUME PLAYBACK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.45f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = targetSong.title ?: defaultTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black withNight Color.White
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "${targetSong.artistsName ?: defaultArtistsName} • ${targetSong.album ?: defaultAlbum}",
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Pure White Tactile Play Button (Apple Music Luxury Standard)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black withNight Color.White)
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
                    modifier = Modifier.size(16.dp),
                    tint = Color.White withNight Color(0xFF0C0D14)
                )
            }
        }
    }
}

/**
 * 3. Quick Picks: 3-Track Stack inside a unified glass container (NO individual grey capsules!)
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
            Column {
                Text(
                    text = stringResource(id = R.string.home_quick_picks),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color.Black withNight Color.White
                )
                Text(
                    text = "Tracks you might like",
                    fontSize = 12.sp,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.45f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF0F0F4) withNight Color(0xFF1B1D2A))
                    .border(0.6.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                    .clickable {
                        if (songs.isNotEmpty()) {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(songs.first(), songs)
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PLAY ALL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black withNight Color.White,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(320.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val pageSongs = pages[pageIndex]
            val cardShape = RoundedCornerShape(18.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .clip(cardShape)
                    .background(Color(0xFFF7F7FA) withNight Color(0xFF141622))
                    .border(
                        0.6.dp,
                        Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.08f),
                        cardShape
                    )
            ) {
                pageSongs.forEachIndexed { idx, song ->
                    QuickPickItem(
                        song = song,
                        onClick = {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(song, songs)
                            }
                        }
                    )
                    if (idx < pageSongs.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 66.dp, end = 14.dp),
                            thickness = 0.5.dp,
                            color = Color.Black.copy(alpha = 0.04f) withNight Color.White.copy(alpha = 0.06f)
                        )
                    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E202C))
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title ?: defaultTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black withNight Color.White
            )
            Spacer(modifier = Modifier.height(1.dp))
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
                .size(14.dp)
                .alpha(0.45f),
            tint = Color.Black withNight Color.White
        )
    }
}

/**
 * 4. Heavy Rotation: 2-Row Horizontal Album Grid with rich artwork and play circle.
 */
@Composable
private fun HeavyRotationSection(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    if (songs.isEmpty()) return

    val heavyRotationAlbums = remember(songs) {
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
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(heavyRotationAlbums) { (albumName, albumSongs) ->
                val firstSong = albumSongs.firstOrNull()
                val cardShape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .width(132.dp)
                        .clip(cardShape)
                        .clickable {
                            Vibrator.click(context)
                            LibraryObject.setTargetAlbumName(albumName)
                            navController.toUI(UI.AlbumInfo)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(cardShape)
                            .background(Color(0xFF191B26))
                            .border(0.6.dp, Color.White.copy(0.08f), cardShape)
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

                        // Floating play circle
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(7.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.65f))
                                .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                                .clickable {
                                    if (albumSongs.isNotEmpty()) {
                                        Vibrator.click(context)
                                        scope.launch(Dispatchers.IO) {
                                            MediaController.prepare(albumSongs.first(), albumSongs)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nowplaying_mp_play),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color.White
                            )
                        }
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
                        text = "${firstSong?.artistsName ?: defaultArtistsName} • ${albumSongs.size}",
                        fontSize = 11.5.sp,
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
        Text(
            text = stringResource(id = R.string.home_recently_added),
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = Color.Black withNight Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(recentAlbums) { song ->
                val cardShape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .width(132.dp)
                        .clip(cardShape)
                        .clickable {
                            Vibrator.click(context)
                            LibraryObject.setTargetAlbumName(song.album ?: defaultAlbum)
                            navController.toUI(UI.AlbumInfo)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(cardShape)
                            .background(Color(0xFF191B26))
                            .border(0.6.dp, Color.White.copy(0.08f), cardShape)
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
                        fontSize = 11.5.sp,
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
 * 6. Hero Recommendations Carousel ("Try These").
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
                color = Color.Black withNight Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            val scope = rememberCoroutineScope()

            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(290.dp),
                contentPadding = PaddingValues(start = 18.dp, end = 50.dp),
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
        val currentAlbum = currentSong.album

        if (currentAlbum.isNullOrEmpty() || currentAlbum == defaultAlbum) {
            groupedItems.add(RecentlyPlayedDisplayItem.Song(currentSong))
            currentIndex++
        } else {
            val contiguousRun = mutableListOf(currentSong)
            var peekIndex = currentIndex + 1
            while (peekIndex < rawHistory.size && rawHistory[peekIndex].album == currentAlbum) {
                contiguousRun.add(rawHistory[peekIndex])
                peekIndex++
            }

            if (contiguousRun.size >= 2) {
                groupedItems.add(
                    RecentlyPlayedDisplayItem.AlbumGroup(
                        albumName = currentAlbum,
                        artistName = currentSong.albumArtists ?: currentSong.artistsName ?: defaultArtistsName,
                        artworkUri = currentSong.thumb,
                        songs = contiguousRun.toList()
                    )
                )
            } else {
                groupedItems.add(RecentlyPlayedDisplayItem.Song(currentSong))
            }
            currentIndex = peekIndex
        }

        if (groupedItems.size >= maximumDisplayCount) {
            break
        }
    }

    return groupedItems.take(maximumDisplayCount)
}

@Composable
fun RecentlyPlayedCard() {
    val history = ListenHistoryManager.recentlyPlayedSongs.value
    val displayItems = remember(history) {
        buildRecentlyPlayedDisplayItems(history, maximumDisplayCount = 10)
    }

    if (displayItems.isNotEmpty()) {
        val scope = rememberCoroutineScope()

        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_recently_played),
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.Black withNight Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    count = displayItems.size,
                    key = { index ->
                        when (val item = displayItems[index]) {
                            is RecentlyPlayedDisplayItem.Song -> "song_${item.song.uri ?: item.song.mediaId}_$index"
                            is RecentlyPlayedDisplayItem.AlbumGroup -> "album_${item.albumName}_$index"
                        }
                    }
                ) { index ->
                    when (val item = displayItems[index]) {
                        is RecentlyPlayedDisplayItem.Song -> {
                            val song = item.song
                            val cardShape = RoundedCornerShape(14.dp)
                            Column(
                                modifier = Modifier
                                    .width(132.dp)
                                    .clip(cardShape)
                                    .clickable {
                                        scope.launch(Dispatchers.IO) {
                                            MediaController.prepare(song, history)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(132.dp)
                                        .clip(cardShape)
                                        .background(Color(0xFF191B26))
                                        .border(0.6.dp, Color.White.copy(0.08f), cardShape)
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
                                    text = song.title ?: defaultTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.Black withNight Color.White
                                )
                                Text(
                                    text = song.artistsName ?: defaultArtistsName,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
                                )
                            }
                        }
                        is RecentlyPlayedDisplayItem.AlbumGroup -> {
                            val cardShape = RoundedCornerShape(14.dp)
                            Column(
                                modifier = Modifier
                                    .width(132.dp)
                                    .clip(cardShape)
                                    .clickable {
                                        scope.launch(Dispatchers.IO) {
                                            MediaController.prepare(item.songs.first(), item.songs)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(132.dp)
                                        .clip(cardShape)
                                        .background(Color(0xFF191B26))
                                        .border(0.6.dp, Color.White.copy(0.08f), cardShape)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(item.artworkUri)
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
                                    text = item.albumName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.Black withNight Color.White
                                )
                                Text(
                                    text = "${item.artistName} • ${item.songs.size} tracks",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cinematic Hero Card with full-bleed curved imagery and bottom gradient scrim.
 */
@Composable
fun RecommendCardItem(music: YosMediaItem, onClick: () -> Unit) =
    Column(
        Modifier
            .width(280.dp)
            .padding(end = 12.dp)
    ) {
        val shape = RoundedCornerShape(20.dp)

        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(shape)
                .background(Color(0xFF161824))
                .border(0.6.dp, Color.White.copy(alpha = 0.10f), shape)
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
                modifier = Modifier.fillMaxSize()
            )

            // Cinematic gradient scrim overlay at the bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x33000000),
                                Color(0xCC0C0D14),
                                Color(0xF50C0D14)
                            )
                        )
                    )
            )

            // Overlaid typography & play button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FEATURED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = music.title ?: defaultTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${music.artistsName ?: defaultArtistsName} • ${music.album ?: defaultAlbum}",
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nowplaying_mp_play),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF0C0D14)
                    )
                }
            }
        }
    }
