package yos.music.player.ui.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun HomeScreen(
    featured: List<FeaturedItem>,
    recentlyPlayed: List<FeaturedItem>,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onItemClick: (FeaturedItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // simple gradient background; later we can switch to color extraction
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color.Black, Color(0xFF0F0F0F)))
            ))

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "Home", style = MaterialTheme.typography.headlineLarge, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Featured Picks", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(12.dp))

            HeroCarousel(items = featured, onItemClick = onItemClick)

            Spacer(modifier = Modifier.height(18.dp))
            Text(text = "Recently Played", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(start = 20.dp))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp)) {
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentlyPlayed) { item ->
                        SmallAlbumCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            MiniPlayer(title = recentlyPlayed.firstOrNull()?.title ?: "—", isPlaying = isPlaying, onPlayPause = onPlayPause)
        }
    }
}

@Composable
fun HeroCarousel(items: List<FeaturedItem>, onItemClick: (FeaturedItem) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items) { item ->
            FeaturedCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
fun FeaturedCard(item: FeaturedItem, onClick: () -> Unit) {
    val cardWidth = 300.dp
    val cardHeight = 180.dp
    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(item.art).crossfade(true).build(),
                contentDescription = "${item.title} artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))))

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(text = item.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(text = item.artist, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SmallAlbumCard(item: FeaturedItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(item.art).crossfade(true).build(),
            contentDescription = "${item.title} art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .shadow(4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = item.title, maxLines = 1, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MiniPlayer(title: String, isPlaying: Boolean, onPlayPause: () -> Unit) {
    Surface(
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = Color(0xFF111111),
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Box(modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onPlayPause) {
                if (isPlaying) Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                else Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
            }
        }
    }
}

// simple data holder

data class FeaturedItem(val id: String, val title: String, val artist: String, val art: String)
