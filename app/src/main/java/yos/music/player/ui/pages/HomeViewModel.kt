package yos.music.player.ui.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _featured = MutableStateFlow<List<FeaturedItem>>(emptyList())
    val featured: StateFlow<List<FeaturedItem>> = _featured

    private val _recent = MutableStateFlow<List<FeaturedItem>>(emptyList())
    val recent: StateFlow<List<FeaturedItem>> = _recent

    private val sample = listOf(
        FeaturedItem("1", "Champion", "BLACKPINK", "https://picsum.photos/600/600?1"),
        FeaturedItem("2", "Salty & Sweet", "aespa", "https://picsum.photos/600/600?2"),
        FeaturedItem("3", "Anxiety", "Unknown", "https://picsum.photos/600/600?3")
    )

    init {
        viewModelScope.launch {
            // In later commits we'll replace this with real library data
            _featured.emit(sample)
            _recent.emit(sample)
        }
    }
}
