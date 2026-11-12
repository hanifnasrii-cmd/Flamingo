package yos.music.player.ui.pages.library

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
import yos.music.player.code.ListenStatsManager
import yos.music.player.code.MediaController
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.data.libraries.FavPlayListLibrary
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultAlbum
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.libraries.defaultTitle
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.YosWrapper

enum class LibrarySortOption {
    DateAdded,
    Title,
    Artist
}

data class LibraryAlbumItem(
    val name: String,
    val artist: String,
    val artworkUri: Uri?,
    val addDate: Long,
    val songs: List<YosMediaItem>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Library(navController: NavController) {
    val songs = runCatching { MusicLibrary.songs }.getOrDefault(emptyList())
    var sortOption by remember { mutableStateOf(LibrarySortOption.DateAdded) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Group songs into distinct albums
    val albumList = remember(songs, sortOption) {
        val grouped = songs.groupBy { it.album ?: defaultAlbum }.map { (albumName, albumSongs) ->
            val first = albumSongs.firstOrNull()
            LibraryAlbumItem(
                name = albumName,
                artist = first?.albumArtists ?: first?.artistsName ?: defaultArtistsName,
                artworkUri = first?.thumb,
                addDate = albumSongs.maxOfOrNull { it.addDate ?: 0L } ?: 0L,
                songs = albumSongs
            )
        }
        when (sortOption) {
            LibrarySortOption.DateAdded -> grouped.sortedByDescending { it.addDate }
            LibrarySortOption.Title -> grouped.sortedBy { it.name.lowercase() }
            LibrarySortOption.Artist -> grouped.sortedBy { it.artist.lowercase() }
        }
    }

    Title(
        title = stringResource(id = R.string.page_library_title),
        rightIconContent = {
            ProfileButton { navController.toUI(UI.Settings.Main) }
        },
        content = {
            // 1. Modern Grouped Cupertino Navigation Card
            item("NavGroup") {
                LibraryNavigationGroup(navController = navController, songs = songs)
            }

            // 2. Smart Collections Carousel
            item("SmartCollections") {
                SmartCollectionsRow(navController = navController, allSongs = songs)
            }

            // 3. Recently Added Header with Sort Controls
            item("RecentlyAddedHeader") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.library_recently_added_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black withNight Color.White
                    )

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF0F0F2) withNight Color(0xFF1E1E22))
                                .clickable { sortDropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (sortOption) {
                                    LibrarySortOption.DateAdded -> stringResource(id = R.string.normal_button_sort_by_date)
                                    LibrarySortOption.Title -> stringResource(id = R.string.normal_button_sort_by_name)
                                    LibrarySortOption.Artist -> stringResource(id = R.string.normal_button_sort_by_artist)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_next),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(0.6f),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = sortDropdownExpanded,
                            onDismissRequest = { sortDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.normal_button_sort_by_date)) },
                                onClick = {
                                    sortOption = LibrarySortOption.DateAdded
                                    sortDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.normal_button_sort_by_name)) },
                                onClick = {
                                    sortOption = LibrarySortOption.Title
                                    sortDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.normal_button_sort_by_artist)) },
                                onClick = {
                                    sortOption = LibrarySortOption.Artist
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 4. Responsive 2-Column Album Grid
            val chunkedAlbums = albumList.chunked(2)
            items(chunkedAlbums, key = { row -> row.joinToString("-") { it.name } }) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LibraryAlbumCard(
                        album = pair[0],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Vibrator.click(context)
                            LibraryObject.setTargetAlbumName(pair[0].name)
                            navController.toUI(UI.AlbumInfo)
                        },
                        onPlayClick = {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                if (pair[0].songs.isNotEmpty()) {
                                    MediaController.prepare(pair[0].songs.first(), pair[0].songs)
                                }
                            }
                        }
                    )

                    if (pair.size > 1) {
                        LibraryAlbumCard(
                            album = pair[1],
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Vibrator.click(context)
                                LibraryObject.setTargetAlbumName(pair[1].name)
                                navController.toUI(UI.AlbumInfo)
                            },
                            onPlayClick = {
                                Vibrator.click(context)
                                scope.launch(Dispatchers.IO) {
                                    if (pair[1].songs.isNotEmpty()) {
                                        MediaController.prepare(pair[1].songs.first(), pair[1].songs)
                                    }
                                }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Bottom breathing space
            item("BottomSpacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    )
}

/**
 * Modern Grouped Cupertino Navigation Card
 */
@Composable
private fun LibraryNavigationGroup(
    navController: NavController,
    songs: List<YosMediaItem>
) {
    val cardBg = Color(0xFFF7F7F9) withNight Color(0xFF161619)
    val cardBorder = Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.07f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(0.6.dp, cardBorder, RoundedCornerShape(16.dp))
    ) {
        // Playlists
        ModernNavRow(
            icon = painterResource(id = R.drawable.ic_library_link_icon_playlists),
            label = stringResource(id = R.string.page_library_playlists),
            onClick = { navController.toUI(UI.PlayLists) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = cardBorder, thickness = 0.5.dp)

        // Artists
        ModernNavRow(
            icon = painterResource(id = R.drawable.ic_library_link_icon_artists),
            label = stringResource(id = R.string.page_library_artists),
            onClick = { navController.toUI(UI.LocalArtists) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = cardBorder, thickness = 0.5.dp)

        // Albums
        ModernNavRow(
            icon = painterResource(id = R.drawable.ic_library_link_icon_album),
            label = stringResource(id = R.string.page_library_albums),
            onClick = { navController.toUI(UI.LocalAlbums) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = cardBorder, thickness = 0.5.dp)

        // Songs
        ModernNavRow(
            icon = painterResource(id = R.drawable.ic_library_link_icon_songs),
            label = stringResource(id = R.string.page_library_songs),
            badge = "${songs.size}",
            onClick = {
                LibraryObject.setTargetListWithTitle("Songs", songs)
                navController.toUI(UI.NormalMusic)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = cardBorder, thickness = 0.5.dp)

        // Favorites
        ModernNavRow(
            icon = painterResource(id = R.drawable.ic_nowplaying_favorited),
            label = stringResource(id = R.string.library_smart_favorites),
            badge = "${FavPlayListLibrary.favPlayList.size}",
            onClick = {
                LibraryObject.setTargetListWithTitle("Favorites", FavPlayListLibrary.favPlayList)
                navController.toUI(UI.NormalMusic)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = cardBorder, thickness = 0.5.dp)

        // Folders
        ModernNavRow(
            icon = painterResource(id = R.drawable.placeholder_folder),
            label = stringResource(id = R.string.library_folders),
            onClick = {
                val folderSongs = MusicLibrary.folders.flatMap { it.songs }.ifEmpty { songs }
                LibraryObject.setTargetListWithTitle("Folders", folderSongs)
                navController.toUI(UI.NormalMusic)
            }
        )
    }
}

@Composable
private fun ModernNavRow(
    icon: Painter,
    label: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black withNight Color.White,
            modifier = Modifier.weight(1f)
        )

        if (badge != null) {
            Text(
                text = badge,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = (Color.Black withNight Color.White).copy(alpha = 0.4f),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_action_next),
            contentDescription = null,
            modifier = Modifier
                .size(12.dp)
                .alpha(0.3f),
            tint = Color.Black withNight Color.White
        )
    }
}

/**
 * Smart Collections Carousel: Favorites, Top 25, Recently Played, Recently Added
 */
@Composable
private fun SmartCollectionsRow(
    navController: NavController,
    allSongs: List<YosMediaItem>
) {
    val favList = FavPlayListLibrary.favPlayList
    val recentPlayedList = ListenHistoryManager.recentlyPlayedSongs.value
    val recentlyAddedList = remember(allSongs) {
        allSongs.sortedByDescending { it.addDate ?: 0L }.take(25)
    }

    val collections = remember(favList, recentPlayedList, recentlyAddedList) {
        listOf(
            SmartCollectionItem(
                title = "Favorites",
                subtitle = "${favList.size} tracks",
                iconRes = R.drawable.ic_nowplaying_favorited,
                accentColor = Color(0xFFE64366),
                onClick = {
                    LibraryObject.setTargetListWithTitle("Favorites", favList)
                    navController.toUI(UI.NormalMusic)
                }
            ),
            SmartCollectionItem(
                title = "Recently Added",
                subtitle = "${recentlyAddedList.size} tracks",
                iconRes = R.drawable.ic_library_link_icon_album,
                accentColor = Color(0xFF007AFF),
                onClick = {
                    LibraryObject.setTargetListWithTitle("Recently Added", recentlyAddedList)
                    navController.toUI(UI.NormalMusic)
                }
            ),
            SmartCollectionItem(
                title = "Recently Played",
                subtitle = "${recentPlayedList.size} tracks",
                iconRes = R.drawable.ic_action_play_next,
                accentColor = Color(0xFF34C759),
                onClick = {
                    LibraryObject.setTargetListWithTitle("Recently Played", recentPlayedList)
                    navController.toUI(UI.NormalMusic)
                }
            )
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(collections) { item ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F7F9) withNight Color(0xFF161619))
                    .border(
                        0.5.dp,
                        Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = item.accentColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = Color.Black withNight Color.White
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 11.5.sp,
                        color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private data class SmartCollectionItem(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val accentColor: Color,
    val onClick: () -> Unit
)

/**
 * 2-Column Album Card with Quick-Play Floating Button
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryAlbumCard(
    album: LibraryAlbumItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onPlayClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF202024))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.artworkUri)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_music_default_artwork)
                    .error(R.drawable.placeholder_music_default_artwork)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Floating quick play circle in bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onPlayClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nowplaying_mp_play),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = album.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black withNight Color.White
        )

        Text(
            text = "${album.artist} • ${album.songs.size}",
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
        )
    }
}
