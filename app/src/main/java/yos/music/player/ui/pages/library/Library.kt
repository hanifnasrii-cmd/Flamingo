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
import yos.music.player.data.libraries.FavPlayListLibrary
import yos.music.player.data.libraries.MusicLibrary
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.libraries.defaultAlbum
import yos.music.player.data.libraries.defaultArtistsName
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.primaryDark
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ProfileButton
import yos.music.player.ui.widgets.basic.Title

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
    var activeFilter by remember { mutableStateOf("All") }
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
            // 1. Sleek Filter Chips Bar
            item("LibraryFilterChips") {
                LibraryFilterChips(
                    activeFilter = activeFilter,
                    onFilterSelected = { filter ->
                        activeFilter = filter
                        when (filter) {
                            "Playlists" -> navController.toUI(UI.PlayLists)
                            "Artists" -> navController.toUI(UI.LocalArtists)
                            "Albums" -> navController.toUI(UI.LocalAlbums)
                            "Songs" -> {
                                LibraryObject.setTargetListWithTitle("Songs", songs)
                                navController.toUI(UI.NormalMusic)
                            }
                            "Favorites" -> {
                                LibraryObject.setTargetListWithTitle("Favorites", FavPlayListLibrary.favPlayList)
                                navController.toUI(UI.NormalMusic)
                            }
                            "Folders" -> {
                                val folderSongs = MusicLibrary.folders.flatMap { it.songs }.ifEmpty { songs }
                                LibraryObject.setTargetListWithTitle("Folders", folderSongs)
                                navController.toUI(UI.NormalMusic)
                            }
                        }
                    }
                )
            }

            // 2. Visual Pin Shelf: 4-Tile Hero Grid (Playlists, Favorites, Artists, All Songs)
            item("QuickAccessGrid") {
                LibraryQuickAccessGrid(navController = navController, songs = songs)
            }

            // 3. Smart Collections Row (Recently Played & Recently Added)
            item("SmartCollections") {
                SmartCollectionsRow(navController = navController, allSongs = songs)
            }

            // 4. Albums Section Header with Glass Sort Dropdown
            item("RecentlyAddedHeader") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.page_library_albums),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black withNight Color.White
                        )
                        Text(
                            text = "${albumList.size} albums in your collection",
                            fontSize = 12.sp,
                            color = (Color.Black withNight Color.White).copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF0F0F4) withNight Color(0xFF1B1D2A))
                                .border(
                                    0.6.dp,
                                    Color.Black.copy(alpha = 0.06f) withNight Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { sortDropdownExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
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
                                color = Color.Black withNight Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_action_next),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(0.6f),
                                tint = Color.Black withNight Color.White
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

            // 5. Responsive 2-Column Album Grid
            val chunkedAlbums = albumList.chunked(2)
            items(chunkedAlbums, key = { row -> row.joinToString("-") { it.name } }) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 7.dp),
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
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    )
}

/**
 * 1. Sleek Filter Chips Bar (All, Playlists, Artists, Albums, Songs, Favorites, Folders)
 */
@Composable
private fun LibraryFilterChips(
    activeFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Playlists", "Artists", "Albums", "Songs", "Favorites", "Folders")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == activeFilter
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
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 15.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filter,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        Color.White withNight Color(0xFF0C0D14)
                    } else {
                        Color.Black withNight Color(0xFFD6D6DE)
                    }
                )
            }
        }
    }
}

/**
 * 2. Visual Pin Shelf: 4-Tile Hero Grid (Playlists, Favorites, Artists, All Songs)
 */
@Composable
private fun LibraryQuickAccessGrid(
    navController: NavController,
    songs: List<YosMediaItem>
) {
    val favCount = FavPlayListLibrary.favPlayList.size
    val artistCount = remember(songs) {
        songs.mapNotNull { it.albumArtists ?: it.artistsName }.distinct().size
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Row: Playlists & Favorites
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VisualPinCard(
                title = stringResource(id = R.string.page_library_playlists),
                subtitle = "Custom mixes",
                iconRes = R.drawable.ic_library_link_icon_playlists,
                gradient = Brush.linearGradient(listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))),
                modifier = Modifier.weight(1f),
                onClick = { navController.toUI(UI.PlayLists) }
            )

            VisualPinCard(
                title = stringResource(id = R.string.library_smart_favorites),
                subtitle = "$favCount tracks",
                iconRes = R.drawable.ic_nowplaying_favorited,
                gradient = Brush.linearGradient(listOf(Color(0xFFFF3358), Color(0xFFFF6B45))),
                modifier = Modifier.weight(1f),
                onClick = {
                    LibraryObject.setTargetListWithTitle("Favorites", FavPlayListLibrary.favPlayList)
                    navController.toUI(UI.NormalMusic)
                }
            )
        }

        // Bottom Row: Artists & Songs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VisualPinCard(
                title = stringResource(id = R.string.page_library_artists),
                subtitle = "$artistCount artists",
                iconRes = R.drawable.ic_library_link_icon_artists,
                gradient = Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
                modifier = Modifier.weight(1f),
                onClick = { navController.toUI(UI.LocalArtists) }
            )

            VisualPinCard(
                title = stringResource(id = R.string.page_library_songs),
                subtitle = "${songs.size} tracks",
                iconRes = R.drawable.ic_library_link_icon_songs,
                gradient = Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
                modifier = Modifier.weight(1f),
                onClick = {
                    LibraryObject.setTargetListWithTitle("Songs", songs)
                    navController.toUI(UI.NormalMusic)
                }
            )
        }
    }
}

@Composable
private fun VisualPinCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(cardShape)
            .background(Color(0xFFF7F7FA) withNight Color(0xFF151724))
            .border(
                0.6.dp,
                Color.Black.copy(0.06f) withNight Color.White.copy(0.09f),
                cardShape
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Color.Black withNight Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3. Smart Collections Row
 */
@Composable
private fun SmartCollectionsRow(
    navController: NavController,
    allSongs: List<YosMediaItem>
) {
    val recentPlayedList = ListenHistoryManager.recentlyPlayedSongs.value
    val recentlyAddedList = remember(allSongs) {
        allSongs.sortedByDescending { it.addDate ?: 0L }.take(25)
    }

    val collections = remember(recentPlayedList, recentlyAddedList) {
        listOf(
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
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(collections) { item ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF7F7FA) withNight Color(0xFF141622))
                    .border(
                        0.5.dp,
                        Color.Black.copy(alpha = 0.05f) withNight Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = item.accentColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
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
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onPlayClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cardShape)
                .background(Color(0xFF191B26))
                .border(0.6.dp, Color.White.copy(alpha = 0.08f), cardShape)
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

            // Floating frosted play circle on bottom-right of artwork
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.25f), CircleShape)
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
                    modifier = Modifier.size(15.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = album.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black withNight Color.White
        )

        Text(
            text = "${album.artist} • ${album.songs.size} tracks",
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = (Color.Black withNight Color.White).copy(alpha = 0.5f)
        )
    }
}
